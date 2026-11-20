package com.gaeddong.footy.domain.lineup.presentation;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 직접 팀 편성 API를 보안 필터·예외 처리기·실제 저장과 함께 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LineupTeamUpdateApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private EntityManager entityManager;

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

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    private static final LocalDateTime JOINED_AT = LocalDateTime.of(2026, 11, 20, 12, 0);

    private Member host;
    private Member other;
    private Game game;
    private Lineup lineup;
    private Team teamA;
    private Team teamB;
    private GamePlayer p1;
    private GamePlayer p2;
    private GamePlayer p3;
    private GamePlayer p4;
    private GamePlayer p5;
    private TeamMember m1;
    private TeamMember m2;
    private TeamMember m3;
    private TeamMember m4;
    private TeamMember m5;

    /**
     * A팀: 선수1(파티장, 실력 9, FW), 선수2(6, GK), 선수5(비회원 후보, 3, MF) / B팀: 선수3(8, MF), 선수4(4, DF).
     * 합계는 A 15, B 12(후보 제외)이고 선수1·선수4에는 배치 좌표가 있다.
     */
    @BeforeEach
    void setUp() {
        host = saveMember("host");
        other = saveMember("other");
        game = saveGame();
        p1 = savePlayer(game, host, host.getNickname(), 0, 9, false);
        p2 = savePlayer(game, other, other.getNickname(), 1, 6, false);
        p3 = savePlayer(game, saveMember("p3"), "p3", 2, 8, false);
        p4 = savePlayer(game, saveMember("p4"), "p4", 3, 4, false);
        p5 = savePlayer(game, null, "손님", 4, 3, true);
        lineup = lineupRepository.save(Lineup.builder()
                .game(game)
                .version(1)
                .method(LineupMethod.ALGORITHM)
                .createdAt(JOINED_AT)
                .build());
        teamA = teamRepository.save(Team.builder().lineup(lineup).name("A팀").totalSkill(15).formation("4-4-2").build());
        teamB = teamRepository.save(Team.builder().lineup(lineup).name("B팀").totalSkill(12).build());
        m1 = saveMember(teamA, p1, Position.FW);
        m2 = saveMember(teamA, p2, Position.GK);
        m3 = saveMember(teamB, p3, Position.MF);
        m4 = saveMember(teamB, p4, Position.DF);
        m5 = saveMember(teamA, p5, Position.MF);
        m1.place(new BigDecimal("10.00"), new BigDecimal("20.00"));
        m4.place(new BigDecimal("70.00"), new BigDecimal("80.00"));
        entityManager.flush();
        entityManager.clear();
    }

    private Member saveMember(String key) {
        Member member = Member.builder()
                .email(key + "@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-" + key)
                .build();
        member.completeOnboarding(key);
        return memberRepository.saveAndFlush(member);
    }

    private Game saveGame() {
        return gameRepository.save(Game.create(host, "일요일 풋살", LocalDateTime.now().plusDays(3), "풋살장", 12, 2,
                LocalDateTime.now()));
    }

    private GamePlayer savePlayer(Game target, Member member, String name, int order, int skillLevel,
                                  boolean substitute) {
        GamePlayer player = new GamePlayer(target, member, name, JOINED_AT.plusMinutes(order));
        player.changeSkillLevel(skillLevel);
        player.changeSubstitute(substitute);
        return gamePlayerRepository.save(player);
    }

    private TeamMember saveMember(Team team, GamePlayer player, Position position) {
        return teamMemberRepository.save(TeamMember.builder().team(team).gamePlayer(player).position(position).build());
    }

    private ResultActions updateTeams(Member requester, Long gameId, String body) throws Exception {
        return mockMvc.perform(put("/games/{gameId}/lineups/latest/teams", gameId)
                .header("Authorization", "Bearer " + jwtProvider.createAccessToken(requester.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions latest(Member requester, Long gameId) throws Exception {
        return mockMvc.perform(get("/games/{gameId}/lineups/latest", gameId)
                .header("Authorization", "Bearer " + jwtProvider.createAccessToken(requester.getId())));
    }

    private static String roster(Long teamId, Long... gamePlayerIds) {
        String ids = String.join(",", java.util.Arrays.stream(gamePlayerIds).map(String::valueOf).toList());
        return "{\"teamId\": %d, \"gamePlayerIds\": [%s]}".formatted(teamId, ids);
    }

    private static String body(String... rosters) {
        return "{\"teams\": [%s]}".formatted(String.join(",", rosters));
    }

    // 선수1과 선수3을 서로 바꾼다. A팀 = 선수2·3·5, B팀 = 선수1·4
    private String swapP1AndP3() {
        return body(roster(teamA.getId(), p2.getId(), p3.getId(), p5.getId()),
                roster(teamB.getId(), p1.getId(), p4.getId()));
    }

    private void assertNothingChanged() {
        entityManager.flush();
        entityManager.clear();
        assertThat(teamMemberRepository.findAllByTeamIdOrderByIdAsc(teamA.getId()))
                .extracting(member -> member.getGamePlayer().getId())
                .containsExactly(p1.getId(), p2.getId(), p5.getId());
        assertThat(teamMemberRepository.findAllByTeamIdOrderByIdAsc(teamB.getId()))
                .extracting(member -> member.getGamePlayer().getId())
                .containsExactly(p3.getId(), p4.getId());
        assertThat(teamRepository.findById(teamA.getId()).orElseThrow().getTotalSkill()).isEqualTo(15);
        assertThat(teamRepository.findById(teamB.getId()).orElseThrow().getTotalSkill()).isEqualTo(12);
        assertThat(teamMemberRepository.findById(m1.getId()).orElseThrow().getPosX()).isEqualByComparingTo("10.00");
    }

    @Test
    void 파티장이_선수를_옮기면_새_팀_구성과_다시_계산한_합계를_돌려준다() throws Exception {
        updateTeams(host, game.getId(), swapP1AndP3())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineupId").value(lineup.getId()))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.method").value("ALGORITHM"))
                .andExpect(jsonPath("$.isConfirmed").value(false))
                .andExpect(jsonPath("$.teams", hasSize(2)))
                .andExpect(jsonPath("$.teams[0].teamId").value(teamA.getId()))
                .andExpect(jsonPath("$.teams[0].name").value("A팀"))
                // 6 + 8 (후보 제외)
                .andExpect(jsonPath("$.teams[0].totalSkill").value(14))
                .andExpect(jsonPath("$.teams[0].members", hasSize(3)))
                .andExpect(jsonPath("$.teams[0].members[0].gamePlayerId").value(p2.getId()))
                .andExpect(jsonPath("$.teams[0].members[0].skillLevel").value(6))
                .andExpect(jsonPath("$.teams[0].members[1].gamePlayerId").value(p3.getId()))
                .andExpect(jsonPath("$.teams[0].members[1].position").value("MF"))
                .andExpect(jsonPath("$.teams[0].members[2].gamePlayerId").value(p5.getId()))
                .andExpect(jsonPath("$.teams[0].members[2].isSubstitute").value(true))
                .andExpect(jsonPath("$.teams[1].teamId").value(teamB.getId()))
                // 9 + 4
                .andExpect(jsonPath("$.teams[1].totalSkill").value(13))
                .andExpect(jsonPath("$.teams[1].members", hasSize(2)))
                .andExpect(jsonPath("$.teams[1].members[0].gamePlayerId").value(p1.getId()))
                .andExpect(jsonPath("$.teams[1].members[0].position").value("FW"))
                .andExpect(jsonPath("$.teams[1].members[1].gamePlayerId").value(p4.getId()));
    }

    @Test
    void 응답은_새로_조회한_최신_라인업과_같다() throws Exception {
        String updated = updateTeams(host, game.getId(), swapP1AndP3())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        entityManager.flush();
        entityManager.clear();

        String read = latest(host, game.getId())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(updated).isEqualTo(read);
    }

    @Test
    void 파티장이_아닌_회원은_바뀐_라인업을_보지만_실력은_받지_못한다() throws Exception {
        updateTeams(host, game.getId(), swapP1AndP3()).andExpect(status().isOk());

        latest(other, game.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teams[0].totalSkill").isEmpty())
                .andExpect(jsonPath("$.teams[0].members[1].gamePlayerId").value(p3.getId()))
                .andExpect(jsonPath("$.teams[0].members[1].skillLevel").isEmpty())
                .andExpect(jsonPath("$.teams[1].members[0].gamePlayerId").value(p1.getId()));
    }

    @Test
    void 옮긴_선수의_좌표만_해제하고_나머지는_그대로_둔다() throws Exception {
        updateTeams(host, game.getId(), swapP1AndP3()).andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        TeamMember moved = teamMemberRepository.findById(m1.getId()).orElseThrow();
        assertThat(moved.getTeam().getId()).isEqualTo(teamB.getId());
        assertThat(moved.getPosX()).isNull();
        assertThat(moved.getPosY()).isNull();
        // 배정 포지션과 후보 여부는 그대로다
        assertThat(moved.getPosition()).isEqualTo(Position.FW);
        assertThat(teamMemberRepository.findById(m5.getId()).orElseThrow().getGamePlayer().isSubstitute()).isTrue();
        // 옮기지 않은 선수의 좌표, 팀 포메이션, 라인업 방식은 그대로다
        TeamMember stayed = teamMemberRepository.findById(m4.getId()).orElseThrow();
        assertThat(stayed.getPosX()).isEqualByComparingTo("70.00");
        assertThat(stayed.getPosY()).isEqualByComparingTo("80.00");
        assertThat(teamRepository.findById(teamA.getId()).orElseThrow().getFormation()).isEqualTo("4-4-2");
        assertThat(lineupRepository.findById(lineup.getId()).orElseThrow().getMethod())
                .isEqualTo(LineupMethod.ALGORITHM);
        // 저장된 합계도 바뀐다
        assertThat(teamRepository.findById(teamA.getId()).orElseThrow().getTotalSkill()).isEqualTo(14);
        assertThat(teamRepository.findById(teamB.getId()).orElseThrow().getTotalSkill()).isEqualTo(13);
    }

    @Test
    void 후보를_옮겨도_합계는_그대로다() throws Exception {
        updateTeams(host, game.getId(), body(
                roster(teamA.getId(), p1.getId(), p2.getId()),
                roster(teamB.getId(), p3.getId(), p4.getId(), p5.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teams[0].totalSkill").value(15))
                .andExpect(jsonPath("$.teams[1].totalSkill").value(12))
                .andExpect(jsonPath("$.teams[1].members[2].gamePlayerId").value(p5.getId()))
                .andExpect(jsonPath("$.teams[1].members[2].isSubstitute").value(true));
    }

    @Test
    void 같은_본문을_다시_보내도_같은_결과이고_배치를_또_지우지_않는다() throws Exception {
        String first = updateTeams(host, game.getId(), swapP1AndP3())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // 옮겨진 선수1을 B팀에서 새로 배치했다고 하자
        teamMemberRepository.findById(m1.getId()).orElseThrow()
                .place(new BigDecimal("33.00"), new BigDecimal("44.00"));
        entityManager.flush();
        entityManager.clear();

        String second = updateTeams(host, game.getId(), swapP1AndP3())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(second).isEqualTo(first);
        entityManager.flush();
        entityManager.clear();
        assertThat(teamMemberRepository.findById(m1.getId()).orElseThrow().getPosX()).isEqualByComparingTo("33.00");
    }

    @Test
    void 파티장이_아니면_403이고_아무것도_바뀌지_않는다() throws Exception {
        updateTeams(other, game.getId(), swapP1AndP3())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_403_1"));

        assertNothingChanged();
    }

    @Test
    void 없는_경기면_404이다() throws Exception {
        updateTeams(host, game.getId() + 999, swapP1AndP3())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 삭제된_경기면_404이다() throws Exception {
        game.delete();
        gameRepository.saveAndFlush(game);

        updateTeams(host, game.getId(), swapP1AndP3())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_404_1"));
    }

    @Test
    void 팀을_짜지_않은_경기면_404이다() throws Exception {
        Game noLineup = saveGame();

        updateTeams(host, noLineup.getId(), swapP1AndP3())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LINEUP_404_2"));
    }

    @Test
    void 확정된_라인업이면_409이고_아무것도_바뀌지_않는다() throws Exception {
        lineup.confirm();
        game.confirm();
        lineupRepository.saveAndFlush(lineup);
        gameRepository.saveAndFlush(game);

        updateTeams(host, game.getId(), swapP1AndP3())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LINEUP_409_4"));

        assertNothingChanged();
    }

    @Test
    void 경기가_모집_중이_아니면_409이다() throws Exception {
        game.confirm();
        gameRepository.saveAndFlush(game);

        updateTeams(host, game.getId(), swapP1AndP3())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_409_1"));

        assertNothingChanged();
    }

    @Test
    void 가장_최근_회차의_라인업만_고친다() throws Exception {
        // 회차 2가 최신이 되면 회차 1의 팀은 라인업의 팀이 아니다
        Lineup newer = lineupRepository.save(Lineup.builder()
                .game(game)
                .version(2)
                .method(LineupMethod.ALGORITHM)
                .createdAt(JOINED_AT)
                .build());
        Team newerA = teamRepository.save(Team.builder().lineup(newer).name("A팀").totalSkill(0).build());
        Team newerB = teamRepository.save(Team.builder().lineup(newer).name("B팀").totalSkill(0).build());
        teamMemberRepository.save(TeamMember.builder().team(newerA).gamePlayer(p1).position(Position.FW).build());
        teamMemberRepository.save(TeamMember.builder().team(newerB).gamePlayer(p2).position(Position.GK).build());

        updateTeams(host, game.getId(), swapP1AndP3())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LINEUP_400_3"));

        updateTeams(host, game.getId(), body(roster(newerA.getId(), p2.getId()), roster(newerB.getId(), p1.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineupId").value(newer.getId()))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.teams[0].totalSkill").value(6))
                .andExpect(jsonPath("$.teams[1].totalSkill").value(9));
    }

    @Test
    void 같은_선수를_두_팀에_보내면_400이다() throws Exception {
        updateTeams(host, game.getId(), body(
                roster(teamA.getId(), p1.getId(), p2.getId(), p5.getId()),
                roster(teamB.getId(), p1.getId(), p3.getId(), p4.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LINEUP_400_1"));

        assertNothingChanged();
    }

    @Test
    void 같은_선수를_한_팀에_두_번_보내면_400이다() throws Exception {
        updateTeams(host, game.getId(), body(
                roster(teamA.getId(), p1.getId(), p1.getId(), p2.getId(), p5.getId()),
                roster(teamB.getId(), p3.getId(), p4.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LINEUP_400_1"));
    }

    @Test
    void 라인업에_없는_선수나_다른_경기_선수를_보내면_400이다() throws Exception {
        Game otherGame = saveGame();
        GamePlayer foreign = savePlayer(otherGame, saveMember("foreign"), "다른 경기 선수", 9, 5, false);

        updateTeams(host, game.getId(), body(
                roster(teamA.getId(), p1.getId(), p2.getId(), p5.getId(), foreign.getId()),
                roster(teamB.getId(), p3.getId(), p4.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LINEUP_400_2"));
        updateTeams(host, game.getId(), body(
                roster(teamA.getId(), p1.getId(), p2.getId(), p5.getId()),
                roster(teamB.getId(), p3.getId(), p4.getId() + 9999)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LINEUP_400_2"));

        assertNothingChanged();
    }

    @Test
    void 라인업의_선수를_빠뜨리면_400이다() throws Exception {
        updateTeams(host, game.getId(), body(
                roster(teamA.getId(), p1.getId(), p2.getId(), p5.getId()),
                roster(teamB.getId(), p3.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LINEUP_400_4"));

        assertNothingChanged();
    }

    @Test
    void 팀_목록이_라인업의_팀과_다르면_400이다() throws Exception {
        Game otherGame = saveGame();
        Lineup otherLineup = lineupRepository.save(Lineup.builder()
                .game(otherGame)
                .version(1)
                .method(LineupMethod.ALGORITHM)
                .createdAt(JOINED_AT)
                .build());
        Team foreignTeam = teamRepository.save(Team.builder().lineup(otherLineup).name("A팀").build());

        // 팀이 빠짐
        updateTeams(host, game.getId(), body(
                roster(teamA.getId(), p1.getId(), p2.getId(), p3.getId(), p4.getId(), p5.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LINEUP_400_3"));
        // 다른 경기의 팀
        updateTeams(host, game.getId(), body(
                roster(teamA.getId(), p1.getId(), p2.getId(), p5.getId()),
                roster(foreignTeam.getId(), p3.getId(), p4.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LINEUP_400_3"));
        // 같은 팀이 두 번
        updateTeams(host, game.getId(), body(
                roster(teamA.getId(), p1.getId(), p2.getId(), p5.getId()),
                roster(teamA.getId(), p3.getId(), p4.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LINEUP_400_3"));
        // 빈 목록
        updateTeams(host, game.getId(), "{\"teams\": []}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LINEUP_400_3"));

        assertNothingChanged();
    }

    @Test
    void 한_팀에_선수가_한_명도_없으면_400이다() throws Exception {
        updateTeams(host, game.getId(), body(
                roster(teamA.getId()),
                roster(teamB.getId(), p1.getId(), p2.getId(), p3.getId(), p4.getId(), p5.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LINEUP_400_5"));

        assertNothingChanged();
    }

    @Test
    void 팀에_한_명만_남기는_요청은_받아들인다() throws Exception {
        updateTeams(host, game.getId(), body(
                roster(teamA.getId(), p5.getId()),
                roster(teamB.getId(), p1.getId(), p2.getId(), p3.getId(), p4.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teams[0].members", hasSize(1)))
                .andExpect(jsonPath("$.teams[0].totalSkill").value(0))
                .andExpect(jsonPath("$.teams[1].members", hasSize(4)))
                .andExpect(jsonPath("$.teams[1].totalSkill").value(27));
    }

    @Test
    void 본문_형식이_틀리면_400이다() throws Exception {
        List<String> bodies = List.of(
                "{}",
                "{\"teams\": null}",
                "{\"teams\": [null]}",
                "{\"teams\": [{\"gamePlayerIds\": [1]}]}",
                "{\"teams\": [{\"teamId\": 1}]}",
                "{\"teams\": [{\"teamId\": 1, \"gamePlayerIds\": null}]}",
                "{\"teams\": [{\"teamId\": 1, \"gamePlayerIds\": [null]}]}",
                "{\"teams\": [{\"teamId\": \"가\", \"gamePlayerIds\": []}]}",
                "{\"teams\": [{\"teamId\": 1, \"gamePlayerIds\": \"1\"}]}",
                "{\"teams\": \"A\"}",
                "[]",
                "");
        for (String invalid : bodies) {
            updateTeams(host, game.getId(), invalid)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("GLOBAL_400_1"));
        }

        assertNothingChanged();
    }

    @Test
    void 토큰_없이_요청하면_401이다() throws Exception {
        mockMvc.perform(put("/games/{gameId}/lineups/latest/teams", game.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swapP1AndP3()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GLOBAL_401_1"));

        assertNothingChanged();
    }
}
