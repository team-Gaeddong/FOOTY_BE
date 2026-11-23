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
 * 회원 평점(D-38): 종료된 경기에서 파티장에게 받은 점수의 평균이고, 그런 경기가 2개 이상일 때부터 나온다.
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
    void 평점은_두_번째_경기가_종료될_때_생기고_그_뒤로는_경기가_끝날_때마다_갱신된다() throws Exception {
        Member host = saveMember("host");
        Member player = saveMember("player");

        Game first = saveConfirmedGame(host);
        join(first, player, 7);
        myRating(player).andExpect(jsonPath("$.rating").isEmpty());

        finish(first, host);
        // 한 경기만으로는 평점이 나오지 않는다
        myRating(player).andExpect(jsonPath("$.rating").isEmpty());

        Game second = saveConfirmedGame(host);
        join(second, player, 8);
        // 점수를 받았어도 경기가 끝나기 전에는 반영되지 않는다
        myRating(player).andExpect(jsonPath("$.rating").isEmpty());

        finish(second, host);
        myRating(player).andExpect(jsonPath("$.rating").value(7.5));

        Game third = saveConfirmedGame(host);
        join(third, player, 10);
        finish(third, host);
        // (7 + 8 + 10) / 3 = 8.333… → 소수 둘째 자리 반올림
        myRating(player).andExpect(jsonPath("$.rating").value(8.33));
    }

    @Test
    void 경기를_종료하면_그_경기에_참가한_회원_모두의_평점이_갱신된다() throws Exception {
        Member host = saveMember("host");
        Member player = saveMember("player");
        Member bystander = saveMember("bystander");

        for (int[] scores : new int[][]{{6, 9}, {8, 10}}) {
            Game game = saveConfirmedGame(host);
            join(game, host, scores[0]);
            join(game, player, scores[1]);
            finish(game, host);
        }

        myRating(host).andExpect(jsonPath("$.rating").value(7.0));
        myRating(player).andExpect(jsonPath("$.rating").value(9.5));
        myRating(bystander).andExpect(jsonPath("$.rating").isEmpty());
    }

    @Test
    void 점수를_받지_않은_경기는_평점_계산에서_뺀다() throws Exception {
        Member host = saveMember("host");
        Member player = saveMember("player");

        Game rated = saveConfirmedGame(host);
        join(rated, player, 9);
        finish(rated, host);
        Game unrated = saveConfirmedGame(host);
        join(unrated, player, null);
        finish(unrated, host);
        // 종료된 경기는 둘이지만 점수를 받은 경기는 하나뿐이다
        myRating(player).andExpect(jsonPath("$.rating").isEmpty());

        Game ratedAgain = saveConfirmedGame(host);
        join(ratedAgain, player, 6);
        finish(ratedAgain, host);
        myRating(player).andExpect(jsonPath("$.rating").value(7.5));
    }

    @Test
    void 종료된_경기의_점수를_바꾸면_평점을_다시_계산한다() throws Exception {
        Member host = saveMember("host");
        Member player = saveMember("player");

        Game first = saveConfirmedGame(host);
        join(first, player, 4);
        finish(first, host);
        Game second = saveConfirmedGame(host);
        GamePlayer secondPlayer = join(second, player, 6);
        finish(second, host);
        myRating(player).andExpect(jsonPath("$.rating").value(5.0));

        mockMvc.perform(patch("/games/{gameId}/players/skill-levels", second.getId())
                        .header("Authorization", token(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"players\":[{\"gamePlayerId\":" + secondPlayer.getId() + ",\"skillLevel\":10}]}"))
                .andExpect(status().is2xxSuccessful());

        myRating(player).andExpect(jsonPath("$.rating").value(7.0));
    }

    private ResultActions myRating(Member member) throws Exception {
        return mockMvc.perform(get("/members/me").header("Authorization", token(member)))
                .andExpect(status().isOk());
    }

    private void finish(Game game, Member host) throws Exception {
        mockMvc.perform(patch("/games/{gameId}/finish", game.getId()).header("Authorization", token(host)))
                .andExpect(status().isOk());
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

    private GamePlayer join(Game game, Member member, Integer score) {
        GamePlayer player = new GamePlayer(game, member, member.getNickname(), LocalDateTime.now());
        if (score != null) {
            player.changeSkillLevel(score);
        }
        return gamePlayerRepository.saveAndFlush(player);
    }

    private String token(Member member) {
        return "Bearer " + jwtProvider.createAccessToken(member.getId());
    }
}
