package com.gaeddong.footy.domain.balance.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.balance.domain.BalanceReason;
import com.gaeddong.footy.domain.balance.domain.BalanceReview;
import com.gaeddong.footy.domain.balance.domain.BalanceReviewReason;
import com.gaeddong.footy.domain.balance.domain.repository.BalanceReviewReasonRepository;
import com.gaeddong.footy.domain.balance.domain.repository.BalanceReviewRepository;
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
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BalanceReviewApiTest {

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
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private BalanceReviewRepository balanceReviewRepository;

    @Autowired
    private BalanceReviewReasonRepository balanceReviewReasonRepository;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    @Test
    void 참가_회원은_종료된_경기의_밸런스를_점수_사유_의견과_함께_작성한다() throws Exception {
        Member host = saveMember("host");
        Member player = saveMember("player");
        Game game = saveGame(host, GameStatus.FINISHED);
        join(game, player);

        perform(game.getId(), player, new Request(8,
                List.of(BalanceReason.OUR_TEAM_TOO_WEAK, BalanceReason.SKILL_GAP), "상대 팀 공격수가 너무 강했어요."))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.gameId").value(game.getId()))
                .andExpect(jsonPath("$.score").value(8))
                .andExpect(jsonPath("$.reasons[0]").value("OUR_TEAM_TOO_WEAK"))
                .andExpect(jsonPath("$.reasons[1]").value("SKILL_GAP"))
                .andExpect(jsonPath("$.comment").value("상대 팀 공격수가 너무 강했어요."))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        BalanceReview review = balanceReviewRepository.findAll().getFirst();
        assertThat(review.getWriter().getId()).isEqualTo(player.getId());
        assertThat(review.getScore()).isEqualTo(8);
        assertThat(balanceReviewReasonRepository.findAll())
                .extracting(BalanceReviewReason::getReason)
                .containsExactlyInAnyOrder(BalanceReason.OUR_TEAM_TOO_WEAK, BalanceReason.SKILL_GAP);
    }

    @Test
    void 사유와_의견은_생략할_수_있고_공백_의견은_저장하지_않는다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.FINISHED);
        join(game, host);

        perform(game.getId(), host, new Request(1, null, "   "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.reasons").isEmpty())
                .andExpect(jsonPath("$.comment").isEmpty());

        assertThat(balanceReviewRepository.findAll().getFirst().getComment()).isNull();
        assertThat(balanceReviewReasonRepository.count()).isZero();
    }

    @Test
    void 로그인하지_않으면_401을_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.FINISHED);

        mockMvc.perform(post("/games/{gameId}/balance-reviews", game.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":5}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }

    @Test
    void 경기에_참가하지_않은_회원은_403을_반환한다() throws Exception {
        Member host = saveMember("host");
        Member other = saveMember("other");
        Game game = saveGame(host, GameStatus.FINISHED);

        perform(game.getId(), other, new Request(5, null, null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("BALANCE_403_1"));

        assertThat(balanceReviewRepository.count()).isZero();
    }

    @Test
    void 종료되지_않은_경기는_409를_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.CONFIRMED);
        join(game, host);

        perform(game.getId(), host, new Request(5, null, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BALANCE_409_1"));
    }

    @Test
    void 같은_경기에_두_번_작성하면_409를_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.FINISHED);
        join(game, host);

        perform(game.getId(), host, new Request(5, null, null))
                .andExpect(status().isCreated());
        perform(game.getId(), host, new Request(7, null, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BALANCE_409_2"));

        assertThat(balanceReviewRepository.count()).isEqualTo(1);
    }

    @Test
    void 존재하지_않거나_삭제된_경기는_404를_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.FINISHED);
        join(game, host);
        ReflectionTestUtils.setField(game, "deleted", true);
        gameRepository.flush();

        perform(game.getId(), host, new Request(5, null, null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
        perform(Long.MAX_VALUE, host, new Request(5, null, null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 점수가_1에서_10을_벗어나거나_없으면_400을_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.FINISHED);
        join(game, host);

        for (Integer score : new Integer[]{0, 11, null}) {
            perform(game.getId(), host, new Request(score, null, null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
        }

        assertThat(balanceReviewRepository.count()).isZero();
    }

    @Test
    void 의견이_500자를_넘거나_알_수_없는_사유면_400을_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.FINISHED);
        join(game, host);

        perform(game.getId(), host, new Request(5, null, "가".repeat(501)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
        mockMvc.perform(post("/games/{gameId}/balance-reviews", game.getId())
                        .header("Authorization", token(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":5,\"reasons\":[\"UNKNOWN\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));

        assertThat(balanceReviewRepository.count()).isZero();
    }

    @Test
    void 같은_사유를_중복해서_보내면_400을_반환한다() throws Exception {
        Member host = saveMember("host");
        Game game = saveGame(host, GameStatus.FINISHED);
        join(game, host);

        perform(game.getId(), host, new Request(5,
                List.of(BalanceReason.WELL_BALANCED, BalanceReason.WELL_BALANCED), null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BALANCE_400_1"));

        assertThat(balanceReviewRepository.count()).isZero();
    }

    private ResultActions perform(Long gameId, Member member, Request request) throws Exception {
        return mockMvc.perform(post("/games/{gameId}/balance-reviews", gameId)
                .header("Authorization", token(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
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

    private String token(Member member) {
        return "Bearer " + jwtProvider.createAccessToken(member.getId());
    }

    private record Request(Integer score, List<BalanceReason> reasons, String comment) {
    }
}
