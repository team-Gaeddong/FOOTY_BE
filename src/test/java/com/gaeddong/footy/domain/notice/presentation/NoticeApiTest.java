package com.gaeddong.footy.domain.notice.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.domain.notice.domain.Notice;
import com.gaeddong.footy.domain.notice.domain.repository.NoticeRepository;
import com.gaeddong.footy.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NoticeApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    private Member saveMember(String nickname) {
        String key = UUID.randomUUID().toString();
        Member member = Member.builder()
                .email(key + "@example.com")
                .provider(Provider.GOOGLE)
                .providerId(key)
                .build();
        member.completeOnboarding(nickname);
        return memberRepository.saveAndFlush(member);
    }

    private Game saveGame(Member host) {
        return gameRepository.saveAndFlush(Game.create(host, "주말 풋살", LocalDateTime.now().plusDays(2),
                "풋살장", 10, 2, LocalDateTime.now()));
    }

    private String token(Member member) {
        return "Bearer " + jwtProvider.createAccessToken(member.getId());
    }

    @Test
    void 파티장은_공지사항을_생성하고_응답과_DB에_저장한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host);

        mockMvc.perform(post("/games/{gameId}/notices", game.getId())
                        .header("Authorization", token(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Request("준비물 안내", "운동화를 챙겨 주세요."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("준비물 안내"))
                .andExpect(jsonPath("$.content").value("운동화를 챙겨 주세요."))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        Notice notice = noticeRepository.findAll().getFirst();
        assertThat(notice.getGame().getId()).isEqualTo(game.getId());
        assertThat(notice.getWriter().getId()).isEqualTo(host.getId());
    }

    @Test
    void 로그인하지_않으면_401을_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host);

        mockMvc.perform(post("/games/{gameId}/notices", game.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"공지\",\"content\":\"내용\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }

    @Test
    void 파티장이_아니면_403을_반환하고_저장하지_않는다() throws Exception {
        Member host = saveMember("host");
        Member other = saveMember("other");
        Game game = saveGame(host);

        mockMvc.perform(post("/games/{gameId}/notices", game.getId())
                        .header("Authorization", token(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"공지\",\"content\":\"내용\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_403_1"));

        assertThat(noticeRepository.count()).isZero();
    }

    @Test
    void 존재하지_않거나_삭제된_경기는_404를_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host);
        ReflectionTestUtils.setField(game, "deleted", true);
        gameRepository.flush();

        mockMvc.perform(post("/games/{gameId}/notices", game.getId())
                        .header("Authorization", token(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"공지\",\"content\":\"내용\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
        mockMvc.perform(post("/games/{gameId}/notices", Long.MAX_VALUE)
                        .header("Authorization", token(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"공지\",\"content\":\"내용\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 제목이_100자를_초과하거나_필드가_비어_있으면_400을_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host);

        mockMvc.perform(post("/games/{gameId}/notices", game.getId())
                        .header("Authorization", token(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Request("가".repeat(101), "내용"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
        mockMvc.perform(post("/games/{gameId}/notices", game.getId())
                        .header("Authorization", token(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Request(" ", "내용"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
        mockMvc.perform(post("/games/{gameId}/notices", game.getId())
                        .header("Authorization", token(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Request("제목", "  \n "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));

        assertThat(noticeRepository.count()).isZero();
    }

    private record Request(String title, String content) {
    }
}
