package com.gaeddong.footy.domain.game.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.domain.repository.PlayerPositionRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 경기별 선수 포지션 설정 API를 보안 필터·예외 처리기와 함께 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlayerPositionApiTest {

    private static final String BODY = "{\"positions\":[\"DF\",\"GK\"]}";

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
    private PlayerPositionRepository playerPositionRepository;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    private Member saveMember(String email, String providerId) {
        Member member = Member.builder()
                .email(email)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .build();
        member.completeOnboarding("회원");
        return memberRepository.saveAndFlush(member);
    }

    private Game saveGame(Member host) {
        return gameRepository.save(Game.create(host, "일요일 풋살", LocalDateTime.now().plusDays(3), "풋살장", 12, 2,
                LocalDateTime.now()));
    }

    private ResultActions putPositions(Member requester, Long gameId, Long gamePlayerId, String body) throws Exception {
        return mockMvc.perform(put("/games/{gameId}/players/{gamePlayerId}/positions", gameId, gamePlayerId)
                .header("Authorization", "Bearer " + jwtProvider.createAccessToken(requester.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void 파티장이_선수_포지션을_설정하면_204() throws Exception {
        Member host = saveMember("host@example.com", "google-host");
        Game game = saveGame(host);
        GamePlayer player = gamePlayerRepository.save(new GamePlayer(game, null, "비회원"));

        putPositions(host, game.getId(), player.getId(), BODY)
                .andExpect(status().isNoContent());

        assertThat(playerPositionRepository.findAllByGamePlayerIdOrderByPriorityAsc(player.getId()))
                .extracting(PlayerPosition::getPosition, PlayerPosition::getPriority)
                .containsExactly(tuple(Position.DF, 1), tuple(Position.GK, 2));
    }

    @Test
    void 파티장이_아니면_403() throws Exception {
        Member host = saveMember("host@example.com", "google-host");
        Member other = saveMember("other@example.com", "google-other");
        Game game = saveGame(host);
        GamePlayer player = gamePlayerRepository.save(new GamePlayer(game, other, "회원"));

        putPositions(other, game.getId(), player.getId(), BODY)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_403_1"));
    }

    @Test
    void 다른_경기의_참가자면_404() throws Exception {
        Member host = saveMember("host@example.com", "google-host");
        Game game = saveGame(host);
        Game otherGame = saveGame(host);
        GamePlayer otherPlayer = gamePlayerRepository.save(new GamePlayer(otherGame, null, "비회원"));

        putPositions(host, game.getId(), otherPlayer.getId(), BODY)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_2"));
    }

    @Test
    void 없는_경기면_404() throws Exception {
        Member host = saveMember("host@example.com", "google-host");

        putPositions(host, 999_999L, 1L, BODY)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 포지션_목록이_잘못되면_400() throws Exception {
        Member host = saveMember("host@example.com", "google-host");
        Game game = saveGame(host);
        GamePlayer player = gamePlayerRepository.save(new GamePlayer(game, null, "비회원"));

        putPositions(host, game.getId(), player.getId(), "{\"positions\":[\"FW\",\"FW\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MEMBER_400_2"));
    }

    @Test
    void 토큰_없이_호출하면_401() throws Exception {
        mockMvc.perform(put("/games/1/players/1/positions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }
}
