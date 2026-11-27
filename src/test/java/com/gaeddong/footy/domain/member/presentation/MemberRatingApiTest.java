package com.gaeddong.footy.domain.member.presentation;

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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 회원 평점(D-38, D-41): 종료된 경기에서 파티장에게 받은 경기 후 평점의 평균이고, 그런 경기가 2개 이상일 때부터 나온다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberRatingApiTest {

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
    void 평점은_두_번째_경기의_평점을_받을_때_생기고_그_뒤로는_평점을_받을_때마다_갱신된다() throws Exception {
        Member host = saveMember("host");
        Member player = saveMember("player");

        GamePlayer first = join(saveFinishedGame(host), player);
        myRating(player).andExpect(jsonPath("$.rating").isEmpty());

        rate(first, host, "7");
        // 한 경기만으로는 평점이 나오지 않는다
        myRating(player).andExpect(jsonPath("$.rating").isEmpty());

        GamePlayer second = join(saveFinishedGame(host), player);
        // 경기가 끝났어도 평점을 받기 전에는 반영되지 않는다
        myRating(player).andExpect(jsonPath("$.rating").isEmpty());

        rate(second, host, "8.5");
        myRating(player).andExpect(jsonPath("$.rating").value(7.75));

        rate(join(saveFinishedGame(host), player), host, "10");
        // (7 + 8.5 + 10) / 3 = 8.5
        myRating(player).andExpect(jsonPath("$.rating").value(8.5));

        rate(join(saveFinishedGame(host), player), host, "1.5");
        // (7 + 8.5 + 10 + 1.5) / 4 = 6.75
        myRating(player).andExpect(jsonPath("$.rating").value(6.75));
    }

    @Test
    void 평균은_소수_둘째_자리에서_반올림한다() throws Exception {
        Member host = saveMember("host");
        Member player = saveMember("player");

        for (String rating : new String[]{"7", "8", "10"}) {
            rate(join(saveFinishedGame(host), player), host, rating);
        }
        // (7 + 8 + 10) / 3 = 8.333…
        myRating(player).andExpect(jsonPath("$.rating").value(8.33));
    }

    @Test
    void 이미_준_평점을_고치면_회원_평점도_다시_계산한다() throws Exception {
        Member host = saveMember("host");
        Member player = saveMember("player");

        rate(join(saveFinishedGame(host), player), host, "4");
        GamePlayer second = join(saveFinishedGame(host), player);
        rate(second, host, "6");
        myRating(player).andExpect(jsonPath("$.rating").value(5.0));

        rate(second, host, "10");
        myRating(player).andExpect(jsonPath("$.rating").value(7.0));
    }

    @Test
    void 경기_전_실력은_회원_평점에_쓰지_않는다() throws Exception {
        Member host = saveMember("host");
        Member player = saveMember("player");

        for (int i = 0; i < 2; i++) {
            Game game = saveConfirmedGame(host);
            GamePlayer gamePlayer = join(game, player);
            gamePlayer.changeSkillLevel(9);
            gamePlayerRepository.saveAndFlush(gamePlayer);
            mockMvc.perform(patch("/games/{gameId}/finish", game.getId()).header("Authorization", token(host)))
                    .andExpect(status().isOk());
        }

        myRating(player).andExpect(jsonPath("$.rating").isEmpty());
    }

    private ResultActions myRating(Member member) throws Exception {
        return mockMvc.perform(get("/members/me").header("Authorization", token(member)))
                .andExpect(status().isOk());
    }

    private void rate(GamePlayer player, Member host, String rating) throws Exception {
        mockMvc.perform(patch("/games/{gameId}/players/ratings", player.getGame().getId())
                        .header("Authorization", token(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"players\":[{\"gamePlayerId\":" + player.getId() + ",\"rating\":" + rating + "}]}"))
                .andExpect(status().isNoContent());
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

    private Game saveConfirmedGame(Member host) {
        Game game = Game.create(host, "주말 풋살", LocalDateTime.now().plusDays(2),
                "풋살장", 10, 2, LocalDateTime.now());
        ReflectionTestUtils.setField(game, "status", GameStatus.CONFIRMED);
        return gameRepository.saveAndFlush(game);
    }

    private Game saveFinishedGame(Member host) {
        Game game = saveConfirmedGame(host);
        ReflectionTestUtils.setField(game, "status", GameStatus.FINISHED);
        return gameRepository.saveAndFlush(game);
    }

    private GamePlayer join(Game game, Member member) {
        return gamePlayerRepository.saveAndFlush(new GamePlayer(game, member, member.getNickname(), LocalDateTime.now()));
    }

    private String token(Member member) {
        return "Bearer " + jwtProvider.createAccessToken(member.getId());
    }
}
