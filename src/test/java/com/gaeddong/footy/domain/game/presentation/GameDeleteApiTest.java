package com.gaeddong.footy.domain.game.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GameStatus;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 경기 삭제 API를 보안 필터·예외 처리기와 함께 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GameDeleteApiTest {

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

    private Member saveMember(String email, String providerId, String nickname) {
        Member member = Member.builder()
                .email(email)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .build();
        member.completeOnboarding(nickname);
        return memberRepository.saveAndFlush(member);
    }

    private Game saveGame(Member host, GameStatus status) {
        Game game = Game.create(host, "일요일 풋살", LocalDateTime.now().plusDays(3), "풋살장", 12, 2,
                LocalDateTime.now());
        ReflectionTestUtils.setField(game, "status", status);
        return gameRepository.saveAndFlush(game);
    }

    private ResultActions deleteGame(Member requester, Long gameId) throws Exception {
        return mockMvc.perform(delete("/games/{gameId}", gameId)
                .header("Authorization", "Bearer " + jwtProvider.createAccessToken(requester.getId())));
    }

    @Test
    void 파티장이_모집_중인_경기를_삭제하면_204이고_이후_조회는_404() throws Exception {
        Member host = saveMember("host@example.com", "google-host", "파티장");
        Game game = saveGame(host, GameStatus.RECRUITING);

        deleteGame(host, game.getId())
                .andExpect(status().isNoContent());

        assertThat(gameRepository.findById(game.getId())).get().extracting(Game::isDeleted).isEqualTo(true);
        mockMvc.perform(get("/games/{gameId}", game.getId())
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(host.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 팀이_확정된_경기도_삭제할_수_있다() throws Exception {
        Member host = saveMember("host@example.com", "google-host", "파티장");
        Game game = saveGame(host, GameStatus.CONFIRMED);

        deleteGame(host, game.getId())
                .andExpect(status().isNoContent());
    }

    @Test
    void 종료된_경기면_409() throws Exception {
        Member host = saveMember("host@example.com", "google-host", "파티장");
        Game game = saveGame(host, GameStatus.FINISHED);

        deleteGame(host, game.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_409_5"));
    }

    @Test
    void 파티장이_아니면_403() throws Exception {
        Member host = saveMember("host@example.com", "google-host", "파티장");
        Member other = saveMember("other@example.com", "google-other", "참가자");
        Game game = saveGame(host, GameStatus.RECRUITING);

        deleteGame(other, game.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_403_1"));
    }

    @Test
    void 이미_삭제된_경기면_404() throws Exception {
        Member host = saveMember("host@example.com", "google-host", "파티장");
        Game game = saveGame(host, GameStatus.RECRUITING);
        deleteGame(host, game.getId()).andExpect(status().isNoContent());

        deleteGame(host, game.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 없는_경기면_404() throws Exception {
        Member host = saveMember("host@example.com", "google-host", "파티장");

        deleteGame(host, 999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 토큰_없이_호출하면_401() throws Exception {
        mockMvc.perform(delete("/games/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }
}
