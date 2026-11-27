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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 경기 후 선수 평점(D-41).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlayerRatingApiTest {

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
    void 파티장은_종료된_경기의_선수들에게_평점을_주고_조회한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.FINISHED);
        join(game, host);
        GamePlayer first = join(game, saveMember("first"));
        GamePlayer second = join(game, saveMember("second"));
        GamePlayer unrated = join(game, saveMember("unrated"));

        rate(game, host, "[{\"gamePlayerId\":" + first.getId() + ",\"rating\":7.5},"
                + "{\"gamePlayerId\":" + second.getId() + ",\"rating\":10}]")
                .andExpect(status().isNoContent());

        // 파티장 본인은 평점 대상이 아니라 목록에 없다
        list(game, host)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(game.getId()))
                .andExpect(jsonPath("$.players", hasSize(3)))
                .andExpect(jsonPath("$.players[0].gamePlayerId").value(first.getId()))
                .andExpect(jsonPath("$.players[0].name").value("first"))
                .andExpect(jsonPath("$.players[0].rating").value(7.5))
                .andExpect(jsonPath("$.players[1].rating").value(10.0))
                .andExpect(jsonPath("$.players[2].gamePlayerId").value(unrated.getId()))
                .andExpect(jsonPath("$.players[2].rating").isEmpty());

        // 실력(skill_level)과 별개의 값이다
        assertThat(gamePlayerRepository.findById(first.getId()).orElseThrow().getSkillLevel()).isNull();
    }

    @Test
    void 다시_보내면_평점을_덮어쓴다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.FINISHED);
        GamePlayer player = join(game, saveMember("player"));

        rate(game, host, "[{\"gamePlayerId\":" + player.getId() + ",\"rating\":3.0}]")
                .andExpect(status().isNoContent());
        rate(game, host, "[{\"gamePlayerId\":" + player.getId() + ",\"rating\":8.5}]")
                .andExpect(status().isNoContent());

        assertThat(gamePlayerRepository.findById(player.getId()).orElseThrow().getRating())
                .isEqualByComparingTo(new BigDecimal("8.5"));
    }

    @Test
    void 파티장이_아니면_평점을_주거나_볼_수_없다() throws Exception {
        Member host = saveMember("host");
        Member player = saveMember("player");
        Game game = saveGame(host, GameStatus.FINISHED);
        GamePlayer target = join(game, player);

        rate(game, player, "[{\"gamePlayerId\":" + target.getId() + ",\"rating\":9}]")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_403_1"));
        list(game, player)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_403_1"));
        mockMvc.perform(get("/games/{gameId}/players/ratings", game.getId()))
                .andExpect(status().isUnauthorized());

        assertThat(gamePlayerRepository.findById(target.getId()).orElseThrow().getRating()).isNull();
    }

    @Test
    void 종료되지_않은_경기는_평점을_줄_수_없다() throws Exception {
        Member host = saveMember("host");
        for (GameStatus status : new GameStatus[]{GameStatus.RECRUITING, GameStatus.CONFIRMED}) {
            Game game = saveGame(host, status);
            GamePlayer player = join(game, saveMember("player-" + status));

            rate(game, host, "[{\"gamePlayerId\":" + player.getId() + ",\"rating\":5}]")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("GAME_409_6"));
        }
    }

    @Test
    void 파티장_본인에게는_평점을_줄_수_없고_아무것도_반영하지_않는다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.FINISHED);
        GamePlayer hostPlayer = join(game, host);
        GamePlayer player = join(game, saveMember("player"));

        rate(game, host, "[{\"gamePlayerId\":" + player.getId() + ",\"rating\":6},"
                + "{\"gamePlayerId\":" + hostPlayer.getId() + ",\"rating\":10}]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GAME_400_6"));

        assertThat(gamePlayerRepository.findById(player.getId()).orElseThrow().getRating()).isNull();
    }

    @Test
    void 평점은_1에서_10_사이의_0_5_단위여야_한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.FINISHED);
        GamePlayer player = join(game, saveMember("player"));

        for (String rating : new String[]{"0.5", "10.5", "7.3", "7.25", "0"}) {
            rate(game, host, "[{\"gamePlayerId\":" + player.getId() + ",\"rating\":" + rating + "}]")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("GAME_400_5"));
        }
        for (String rating : new String[]{"1", "1.0", "5.5", "10.0"}) {
            rate(game, host, "[{\"gamePlayerId\":" + player.getId() + ",\"rating\":" + rating + "}]")
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    void 요청_형식이_틀리거나_선수가_중복되거나_다른_경기_선수면_거부한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.FINISHED);
        Game otherGame = saveGame(host, GameStatus.FINISHED);
        GamePlayer player = join(game, saveMember("player"));
        GamePlayer outsider = join(otherGame, saveMember("outsider"));

        rate(game, host, "[]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
        rate(game, host, "[{\"gamePlayerId\":" + player.getId() + "}]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
        rate(game, host, "[{\"gamePlayerId\":" + player.getId() + ",\"rating\":5},"
                + "{\"gamePlayerId\":" + player.getId() + ",\"rating\":6}]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GAME_400_1"));
        rate(game, host, "[{\"gamePlayerId\":" + outsider.getId() + ",\"rating\":5}]")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_2"));
    }

    @Test
    void 비회원_선수에게도_평점을_줄_수_있다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.FINISHED);
        GamePlayer guest = gamePlayerRepository.saveAndFlush(new GamePlayer(game, null, "게스트", LocalDateTime.now()));

        rate(game, host, "[{\"gamePlayerId\":" + guest.getId() + ",\"rating\":6.5}]")
                .andExpect(status().isNoContent());

        list(game, host)
                .andExpect(jsonPath("$.players[0].memberId").isEmpty())
                .andExpect(jsonPath("$.players[0].name").value("게스트"))
                .andExpect(jsonPath("$.players[0].rating").value(6.5));
    }

    private ResultActions rate(Game game, Member requester, String players) throws Exception {
        return mockMvc.perform(patch("/games/{gameId}/players/ratings", game.getId())
                .header("Authorization", token(requester))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"players\":" + players + "}"));
    }

    private ResultActions list(Game game, Member requester) throws Exception {
        return mockMvc.perform(get("/games/{gameId}/players/ratings", game.getId())
                .header("Authorization", token(requester)));
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

    private GamePlayer join(Game game, Member member) {
        return gamePlayerRepository.saveAndFlush(
                new GamePlayer(game, member, member.getNickname(), LocalDateTime.now()));
    }

    private String token(Member member) {
        return "Bearer " + jwtProvider.createAccessToken(member.getId());
    }
}
