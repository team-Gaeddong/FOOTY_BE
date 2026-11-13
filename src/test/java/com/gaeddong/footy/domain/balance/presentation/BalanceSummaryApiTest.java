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
import com.gaeddong.footy.domain.lineup.domain.Lineup;
import com.gaeddong.footy.domain.lineup.domain.LineupMethod;
import com.gaeddong.footy.domain.lineup.domain.Team;
import com.gaeddong.footy.domain.lineup.domain.TeamMember;
import com.gaeddong.footy.domain.lineup.domain.repository.LineupRepository;
import com.gaeddong.footy.domain.lineup.domain.repository.TeamMemberRepository;
import com.gaeddong.footy.domain.lineup.domain.repository.TeamRepository;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BalanceSummaryApiTest {

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
    private LineupRepository lineupRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private BalanceReviewRepository balanceReviewRepository;

    @Autowired
    private BalanceReviewReasonRepository balanceReviewReasonRepository;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    private Member host;
    private Member a2;
    private Member b1;
    private Member b2;
    private Game game;
    private GamePlayer hostPlayer;
    private GamePlayer a2Player;
    private GamePlayer b1Player;
    private GamePlayer b2Player;

    @BeforeEach
    void setUp() {
        host = saveMember("host");
        a2 = saveMember("a2");
        b1 = saveMember("b1");
        b2 = saveMember("b2");
        game = Game.create(host, "주말 풋살", LocalDateTime.now().plusDays(2), "풋살장", 10, 2, LocalDateTime.now());
        ReflectionTestUtils.setField(game, "status", GameStatus.FINISHED);
        game = gameRepository.saveAndFlush(game);
        hostPlayer = join(host);
        a2Player = join(a2);
        b1Player = join(b1);
        b2Player = join(b2);
    }

    @Test
    void 참가_회원이_모두_작성하면_최신_라인업의_팀별_평균을_계산한다() throws Exception {
        // 다시 짜기 전 라인업은 팀 구성이 달라 계산에 쓰지 않는다
        saveLineup(1, List.of(hostPlayer, b1Player), List.of(a2Player, b2Player));
        Lineup latest = saveLineup(2, List.of(hostPlayer, a2Player), List.of(b1Player, b2Player));
        List<Team> teams = teamRepository.findAllByLineupIdOrderByIdAsc(latest.getId());
        review(host, 8, List.of(BalanceReason.WELL_BALANCED), "재밌었어요");
        review(a2, 7, List.of(BalanceReason.OPPONENT_TOO_STRONG, BalanceReason.SKILL_GAP), null);
        review(b1, 3, List.of(BalanceReason.OUR_TEAM_TOO_WEAK, BalanceReason.SKILL_GAP), "공격수가 부족했어요");
        review(b2, 4, List.of(), null);

        perform(game.getId(), a2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(game.getId()))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.requiredCount").value(4))
                .andExpect(jsonPath("$.reviewedCount").value(4))
                .andExpect(jsonPath("$.averageScore").value(5.5))
                .andExpect(jsonPath("$.teams.length()").value(2))
                .andExpect(jsonPath("$.teams[0].teamId").value(teams.get(0).getId()))
                .andExpect(jsonPath("$.teams[0].name").value("A팀"))
                .andExpect(jsonPath("$.teams[0].reviewCount").value(2))
                .andExpect(jsonPath("$.teams[0].averageScore").value(7.5))
                .andExpect(jsonPath("$.teams[0].reasonCounts.WELL_BALANCED").value(1))
                .andExpect(jsonPath("$.teams[0].reasonCounts.SKILL_GAP").value(1))
                .andExpect(jsonPath("$.teams[0].reasonCounts.HEADCOUNT_MISMATCH").value(0))
                .andExpect(jsonPath("$.teams[0].reviews[0].writerId").value(host.getId()))
                .andExpect(jsonPath("$.teams[0].reviews[0].nickname").value("host"))
                .andExpect(jsonPath("$.teams[0].reviews[0].score").value(8))
                .andExpect(jsonPath("$.teams[0].reviews[0].comment").value("재밌었어요"))
                .andExpect(jsonPath("$.teams[0].reviews[1].reasons[0]").value("OPPONENT_TOO_STRONG"))
                .andExpect(jsonPath("$.teams[0].reviews[1].reasons[1]").value("SKILL_GAP"))
                .andExpect(jsonPath("$.teams[1].name").value("B팀"))
                .andExpect(jsonPath("$.teams[1].averageScore").value(3.5))
                .andExpect(jsonPath("$.teams[1].reasonCounts.OUR_TEAM_TOO_WEAK").value(1))
                .andExpect(jsonPath("$.teams[1].reviews[0].nickname").value("b1"))
                .andExpect(jsonPath("$.teams[1].reviews[0].comment").value("공격수가 부족했어요"))
                .andExpect(jsonPath("$.teams[1].reviews[1].reasons").isEmpty());
    }

    @Test
    void 아직_모두_작성하지_않았으면_진행_현황만_반환한다() throws Exception {
        saveLineup(1, List.of(hostPlayer, a2Player), List.of(b1Player, b2Player));
        review(host, 8, List.of(), null);
        review(b1, 3, List.of(), null);

        perform(game.getId(), host)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.requiredCount").value(4))
                .andExpect(jsonPath("$.reviewedCount").value(2))
                .andExpect(jsonPath("$.averageScore").isEmpty())
                .andExpect(jsonPath("$.teams").isEmpty());
    }

    @Test
    void 탈퇴한_회원과_비회원_선수는_작성_대상에서_제외한다() throws Exception {
        GamePlayer guest = gamePlayerRepository.saveAndFlush(new GamePlayer(game, null, "게스트", LocalDateTime.now()));
        saveLineup(1, List.of(hostPlayer, a2Player, guest), List.of(b1Player, b2Player));
        b2.withdraw(LocalDateTime.now());
        memberRepository.flush();
        review(host, 6, List.of(), null);
        review(a2, 6, List.of(), null);
        review(b1, 9, List.of(), null);

        perform(game.getId(), host)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.requiredCount").value(3))
                .andExpect(jsonPath("$.averageScore").value(7.0))
                .andExpect(jsonPath("$.teams[0].averageScore").value(6.0))
                .andExpect(jsonPath("$.teams[1].reviewCount").value(1))
                .andExpect(jsonPath("$.teams[1].averageScore").value(9.0));
    }

    @Test
    void 팀에_배정되지_않은_작성자는_전체_평균에만_넣는다() throws Exception {
        saveLineup(1, List.of(hostPlayer, a2Player), List.of(b1Player));
        review(host, 8, List.of(), null);
        review(a2, 8, List.of(), null);
        review(b1, 4, List.of(), null);
        review(b2, 2, List.of(), null);

        perform(game.getId(), host)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageScore").value(5.5))
                .andExpect(jsonPath("$.teams[0].averageScore").value(8.0))
                .andExpect(jsonPath("$.teams[1].reviewCount").value(1))
                .andExpect(jsonPath("$.teams[1].averageScore").value(4.0));
    }

    @Test
    void 모두_작성했지만_라인업이_없으면_404를_반환한다() throws Exception {
        review(host, 5, List.of(), null);
        review(a2, 5, List.of(), null);
        review(b1, 5, List.of(), null);
        review(b2, 5, List.of(), null);

        perform(game.getId(), host)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LINEUP_404_2"));
    }

    @Test
    void 경기에_참가하지_않은_회원은_403을_반환한다() throws Exception {
        Member other = saveMember("other");

        perform(game.getId(), other)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("BALANCE_403_2"));
    }

    @Test
    void 존재하지_않거나_삭제된_경기는_404를_반환한다() throws Exception {
        perform(Long.MAX_VALUE, host)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));

        ReflectionTestUtils.setField(game, "deleted", true);
        gameRepository.flush();
        perform(game.getId(), host)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 로그인하지_않으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/games/{gameId}/balance-reviews/summary", game.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));
    }

    private ResultActions perform(Long gameId, Member member) throws Exception {
        return mockMvc.perform(get("/games/{gameId}/balance-reviews/summary", gameId)
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

    private GamePlayer join(Member member) {
        return gamePlayerRepository.saveAndFlush(
                new GamePlayer(game, member, member.getNickname(), LocalDateTime.now()));
    }

    private Lineup saveLineup(int version, List<GamePlayer> teamA, List<GamePlayer> teamB) {
        Lineup lineup = lineupRepository.save(Lineup.builder()
                .game(game)
                .version(version)
                .method(LineupMethod.ALGORITHM)
                .createdAt(LocalDateTime.now())
                .build());
        saveTeam(lineup, "A팀", teamA);
        saveTeam(lineup, "B팀", teamB);
        return lineup;
    }

    private void saveTeam(Lineup lineup, String name, List<GamePlayer> players) {
        Team team = teamRepository.save(Team.builder().lineup(lineup).name(name).build());
        teamMemberRepository.saveAll(players.stream()
                .map(player -> TeamMember.builder().team(team).gamePlayer(player).position(Position.FW).build())
                .toList());
    }

    private void review(Member writer, int score, List<BalanceReason> reasons, String comment) {
        BalanceReview review = balanceReviewRepository.save(BalanceReview.builder()
                .game(game)
                .writer(writer)
                .score(score)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build());
        balanceReviewReasonRepository.saveAll(reasons.stream()
                .map(reason -> BalanceReviewReason.builder().balanceReview(review).reason(reason).build())
                .toList());
        balanceReviewRepository.flush();
    }
}
