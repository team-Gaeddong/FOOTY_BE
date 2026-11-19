package com.gaeddong.footy.domain.game.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GameFinishApiTest {

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

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    @Test
    void 파티장은_팀이_확정된_경기를_종료하고_경기_상세를_받는다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.CONFIRMED);
        join(game, host);

        perform(game.getId(), host)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(game.getId()))
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.isHost").value(true))
                .andExpect(jsonPath("$.playerCount").value(1));

        assertThat(gameRepository.findById(game.getId()).orElseThrow().isFinished()).isTrue();
    }

    @Test
    void 로그인하지_않으면_401을_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.CONFIRMED);

        mockMvc.perform(patch("/games/{gameId}/finish", game.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }

    @Test
    void 파티장이_아니면_403을_반환한다() throws Exception {
        Member host = saveMember("host");
        Member player = saveMember("player");
        Game game = saveGame(host, GameStatus.CONFIRMED);
        join(game, player);

        perform(game.getId(), player)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_403_1"));

        assertThat(gameRepository.findById(game.getId()).orElseThrow().getStatus()).isEqualTo(GameStatus.CONFIRMED);
    }

    @Test
    void 팀이_확정되지_않았거나_이미_종료된_경기는_409를_반환한다() throws Exception {
        Member host = saveMember("host");

        for (GameStatus status : new GameStatus[]{GameStatus.RECRUITING, GameStatus.FINISHED}) {
            Game game = saveGame(host, status);
            perform(game.getId(), host)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("GAME_409_4"));
            assertThat(gameRepository.findById(game.getId()).orElseThrow().getStatus()).isEqualTo(status);
        }
    }

    @Test
    void 존재하지_않거나_삭제된_경기는_404를_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.CONFIRMED);
        ReflectionTestUtils.setField(game, "deleted", true);
        gameRepository.flush();

        perform(game.getId(), host)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
        perform(Long.MAX_VALUE, host)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    private ResultActions perform(Long gameId, Member member) throws Exception {
        return mockMvc.perform(patch("/games/{gameId}/finish", gameId)
                .header("Authorization", "Bearer " + jwtProvider.createAccessToken(member.getId())));
    }

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

    private Game saveGame(Member host, GameStatus status) {
        Game game = Game.create(host, "주말 풋살", LocalDateTime.now().plusDays(2),
                "풋살장", 10, 2, LocalDateTime.now());
        ReflectionTestUtils.setField(game, "status", status);
        return gameRepository.saveAndFlush(game);
    }

    private void join(Game game, Member member) {
        gamePlayerRepository.saveAndFlush(new GamePlayer(game, member, member.getNickname(), LocalDateTime.now()));
    }
}
