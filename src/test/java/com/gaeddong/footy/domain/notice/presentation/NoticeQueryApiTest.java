package com.gaeddong.footy.domain.notice.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NoticeQueryApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

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

    private void addParticipant(Game game, Member participant) {
        gamePlayerRepository.saveAndFlush(new GamePlayer(game, participant, participant.getNickname(), LocalDateTime.now()));
    }

    private Notice saveNotice(Game game, Member host, String title, String content, LocalDateTime createdAt) {
        return noticeRepository.saveAndFlush(Notice.builder()
                .game(game)
                .writer(host)
                .title(title)
                .content(content)
                .createdAt(createdAt)
                .build());
    }

    private String token(Member member) {
        return "Bearer " + jwtProvider.createAccessToken(member.getId());
    }

    @Test
    void 경기_참가자는_공지_목록을_최신순으로_조회한다() throws Exception {
        Member host = saveMember("host");
        Member participant = saveMember("participant");
        Game game = saveGame(host);
        addParticipant(game, participant);
        LocalDateTime now = LocalDateTime.now();
        saveNotice(game, host, "먼저 쓴 공지", "먼저 내용", now.minusHours(1));
        saveNotice(game, host, "나중 공지", "나중 내용", now);

        mockMvc.perform(get("/games/{gameId}/notices", game.getId())
                        .header("Authorization", token(participant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices.length()").value(2))
                .andExpect(jsonPath("$.notices[0].title").value("나중 공지"))
                .andExpect(jsonPath("$.notices[0].content").value("나중 내용"))
                .andExpect(jsonPath("$.notices[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.notices[1].title").value("먼저 쓴 공지"));
    }

    @Test
    void 공지가_없으면_빈_목록을_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host);

        mockMvc.perform(get("/games/{gameId}/notices", game.getId())
                        .header("Authorization", token(host)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices").isArray())
                .andExpect(jsonPath("$.notices.length()").value(0));
    }

    @Test
    void 파티장은_공지_상세를_조회한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host);
        Notice notice = saveNotice(game, host, "준비물", "운동화를 챙겨 주세요.", LocalDateTime.now());

        mockMvc.perform(get("/games/{gameId}/notices/{noticeId}", game.getId(), notice.getId())
                        .header("Authorization", token(host)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notice.getId()))
                .andExpect(jsonPath("$.title").value("준비물"))
                .andExpect(jsonPath("$.content").value("운동화를 챙겨 주세요."))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void 로그인하지_않으면_401을_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host);

        mockMvc.perform(get("/games/{gameId}/notices", game.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }

    @Test
    void 경기_참가자가_아니면_목록과_상세_모두_403을_반환한다() throws Exception {
        Member host = saveMember("host");
        Member other = saveMember("other");
        Game game = saveGame(host);
        Notice notice = saveNotice(game, host, "공지", "내용", LocalDateTime.now());

        mockMvc.perform(get("/games/{gameId}/notices", game.getId())
                        .header("Authorization", token(other)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOTICE_403_1"));
        mockMvc.perform(get("/games/{gameId}/notices/{noticeId}", game.getId(), notice.getId())
                        .header("Authorization", token(other)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOTICE_403_1"));
    }

    @Test
    void 없는_경기나_삭제된_경기는_404를_반환한다() throws Exception {
        Member host = saveMember("host");
        Game deleted = saveGame(host);
        ReflectionTestUtils.setField(deleted, "deleted", true);
        gameRepository.flush();

        mockMvc.perform(get("/games/{gameId}/notices", Long.MAX_VALUE)
                        .header("Authorization", token(host)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
        mockMvc.perform(get("/games/{gameId}/notices", deleted.getId())
                        .header("Authorization", token(host)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 없는_공지나_다른_경기의_공지는_404를_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host);
        Game otherGame = saveGame(host);
        Notice notice = saveNotice(otherGame, host, "다른 경기 공지", "다른 내용", LocalDateTime.now());

        mockMvc.perform(get("/games/{gameId}/notices/{noticeId}", game.getId(), Long.MAX_VALUE)
                        .header("Authorization", token(host)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTICE_404_1"));
        mockMvc.perform(get("/games/{gameId}/notices/{noticeId}", game.getId(), notice.getId())
                        .header("Authorization", token(host)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTICE_404_1"));

        assertThat(noticeRepository.count()).isEqualTo(1);
    }
}
