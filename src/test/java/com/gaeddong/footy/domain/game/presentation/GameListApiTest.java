package com.gaeddong.footy.domain.game.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GameListApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameRepository gameRepository;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    @Test
    void 삭제되지_않은_전체_경기를_날짜순으로_페이지_조회한다() throws Exception {
        Member host = saveHost();
        Member otherHost = saveHost("other@example.com", "google-other");
        Game future = saveGame(otherHost, "예정 경기", LocalDateTime.of(2030, 6, 1, 10, 0), false);
        Game past = saveGame(host, "지난 경기", LocalDateTime.of(2020, 6, 1, 10, 0), false);
        saveGame(host, "삭제 경기", LocalDateTime.of(2019, 6, 1, 10, 0), true);
        String token = jwtProvider.createAccessToken(host.getId());

        mockMvc.perform(get("/games")
                        .queryParam("page", "0")
                        .queryParam("size", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games.length()").value(1))
                .andExpect(jsonPath("$.games[0].id").value(past.getId()))
                .andExpect(jsonPath("$.games[0].hostId").value(host.getId()))
                .andExpect(jsonPath("$.games[0].title").value("지난 경기"))
                .andExpect(jsonPath("$.games[0].status").value("RECRUITING"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/games")
                        .queryParam("page", "1")
                        .queryParam("size", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games[0].id").value(future.getId()))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void 토큰_없이_목록을_조회하면_401이다() throws Exception {
        mockMvc.perform(get("/games"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }

    @Test
    void 경기가_없으면_빈_목록을_반환한다() throws Exception {
        Member host = saveHost();

        mockMvc.perform(get("/games")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(host.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void 잘못된_페이지_값은_400이다() throws Exception {
        Member host = saveHost();
        String token = jwtProvider.createAccessToken(host.getId());

        mockMvc.perform(get("/games")
                        .queryParam("page", "-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));

        mockMvc.perform(get("/games")
                        .queryParam("size", "0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));

        mockMvc.perform(get("/games")
                        .queryParam("size", "101")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
    }

    private Member saveHost() {
        return saveHost("host@example.com", "google-host");
    }

    private Member saveHost(String email, String providerId) {
        Member host = Member.builder()
                .email(email)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .build();
        host.completeOnboarding("호스트");
        return memberRepository.saveAndFlush(host);
    }

    private Game saveGame(Member host, String title, LocalDateTime playDate, boolean deleted) {
        Game game = Game.create(host, title, playDate, "풋살장", 12, 2, LocalDateTime.now());
        if (deleted) {
            ReflectionTestUtils.setField(game, "deleted", true);
        }
        return gameRepository.saveAndFlush(game);
    }
}
