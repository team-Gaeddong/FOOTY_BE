package com.gaeddong.footy.domain.lineup.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.domain.repository.PlayerPositionRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.game.service.GameReader;
import com.gaeddong.footy.domain.lineup.domain.Lineup;
import com.gaeddong.footy.domain.lineup.domain.LineupMethod;
import com.gaeddong.footy.domain.lineup.domain.Team;
import com.gaeddong.footy.domain.lineup.domain.TeamMember;
import com.gaeddong.footy.domain.lineup.domain.repository.LineupRepository;
import com.gaeddong.footy.domain.lineup.domain.repository.TeamMemberRepository;
import com.gaeddong.footy.domain.lineup.domain.repository.TeamRepository;
import com.gaeddong.footy.domain.lineup.exception.LineupErrorCode;
import com.gaeddong.footy.domain.lineup.service.LineupService.LineupDetail;
import com.gaeddong.footy.domain.lineup.service.LineupService.TeamAssignment;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 직접 팀 편성(선수를 팀 사이에서 옮기기)의 규칙을 저장소를 목으로 두고 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class LineupTeamUpdateServiceTest {

    private static final Long HOST_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long GAME_ID = 10L;
    private static final Long TEAM_A_ID = 100L;
    private static final Long TEAM_B_ID = 101L;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 12, 0);

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GamePlayerRepository gamePlayerRepository;

    @Mock
    private PlayerPositionRepository playerPositionRepository;

    @Mock
    private LineupRepository lineupRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    private LineupService lineupService;

    private Game game;
    private Lineup lineup;
    private Team teamA;
    private Team teamB;
    private TeamMember m1;
    private TeamMember m2;
    private TeamMember m3;
    private TeamMember m4;
    private TeamMember m5;

    /**
     * A팀: 선수1(실력 9), 선수2(6), 선수5(3, 후보) / B팀: 선수3(8), 선수4(4). 합계는 A 15, B 12다.
     * 선수1·선수4는 배치 좌표가 있다.
     */
    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL);
        lineupService = new LineupService(new GameReader(gameRepository), gamePlayerRepository,
                playerPositionRepository, lineupRepository, teamRepository, teamMemberRepository, clock);

        game = Game.create(member(HOST_ID), "일요일 풋살", NOW.plusDays(3), "대구 풋살장", 12, 2, NOW);
        ReflectionTestUtils.setField(game, "id", GAME_ID);
        lineup = Lineup.builder().game(game).version(2).method(LineupMethod.ALGORITHM).createdAt(NOW).build();
        ReflectionTestUtils.setField(lineup, "id", 52L);
        teamA = team(TEAM_A_ID, "A팀", 15);
        teamB = team(TEAM_B_ID, "B팀", 12);
        GamePlayer p5 = player(5L, 3);
        p5.changeSubstitute(true);
        m1 = member(teamA, player(1L, 9), Position.FW);
        m2 = member(teamA, player(2L, 6), Position.GK);
        m3 = member(teamB, player(3L, 8), Position.MF);
        m4 = member(teamB, player(4L, 4), Position.DF);
        m5 = member(teamA, p5, Position.MF);
        m1.place(new BigDecimal("10.00"), new BigDecimal("20.00"));
        m4.place(new BigDecimal("70.00"), new BigDecimal("80.00"));
    }

    private static Member member(Long id) {
        Member member = Member.builder()
                .email("member" + id + "@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-" + id)
                .build();
        member.completeOnboarding("회원" + id);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private GamePlayer player(Long id, int skillLevel) {
        GamePlayer player = new GamePlayer(game, null, "선수" + id, NOW.plusMinutes(id));
        ReflectionTestUtils.setField(player, "id", id);
        player.changeSkillLevel(skillLevel);
        return player;
    }

    private Team team(Long id, String name, int totalSkill) {
        Team team = Team.builder().lineup(lineup).name(name).totalSkill(totalSkill).build();
        ReflectionTestUtils.setField(team, "id", id);
        return team;
    }

    private static TeamMember member(Team team, GamePlayer player, Position position) {
        TeamMember member = TeamMember.builder().team(team).gamePlayer(player).position(position).build();
        ReflectionTestUtils.setField(member, "id", 1000L + player.getId());
        return member;
    }

    private void givenGame() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
    }

    private void givenLineup() {
        givenGame();
        when(lineupRepository.findFirstByGameIdOrderByVersionDesc(GAME_ID)).thenReturn(Optional.of(lineup));
    }

    private void givenTeams() {
        givenLineup();
        when(teamRepository.findAllByLineupIdOrderByIdAsc(lineup.getId())).thenReturn(List.of(teamA, teamB));
        when(teamMemberRepository.findAllByTeamIdInOrderByIdAsc(List.of(TEAM_A_ID, TEAM_B_ID)))
                .thenReturn(List.of(m1, m2, m3, m4, m5));
    }

    private static TeamAssignment assign(Long teamId, Long... gamePlayerIds) {
        return new TeamAssignment(teamId, List.of(gamePlayerIds));
    }

    private static List<Long> playerIds(List<TeamMember> members) {
        return members.stream().map(member -> member.getGamePlayer().getId()).toList();
    }

    @Test
    void 선수를_다른_팀으로_옮기면_소속이_바뀌고_좌표가_해제되고_합계가_다시_계산된다() {
        givenTeams();

        // 선수1(A, 9)과 선수3(B, 8)을 서로 바꾼다
        LineupDetail detail = lineupService.updateTeams(HOST_ID, GAME_ID, List.of(
                assign(TEAM_A_ID, 2L, 3L, 5L), assign(TEAM_B_ID, 1L, 4L)));

        assertThat(detail.lineup()).isSameAs(lineup);
        assertThat(detail.teams()).hasSize(2);
        assertThat(detail.teams().get(0).team()).isSameAs(teamA);
        assertThat(playerIds(detail.teams().get(0).members())).containsExactly(2L, 3L, 5L);
        assertThat(playerIds(detail.teams().get(1).members())).containsExactly(1L, 4L);
        assertThat(m1.getTeam()).isSameAs(teamB);
        assertThat(m3.getTeam()).isSameAs(teamA);
        // 옮긴 선수는 배치가 해제된다
        assertThat(m1.getPosX()).isNull();
        assertThat(m1.getPosY()).isNull();
        // A: 6 + 8 (후보 선수5는 제외), B: 9 + 4
        assertThat(teamA.getTotalSkill()).isEqualTo(14);
        assertThat(teamB.getTotalSkill()).isEqualTo(13);
    }

    @Test
    void 옮기지_않은_선수의_좌표와_배정_포지션은_그대로다() {
        givenTeams();

        lineupService.updateTeams(HOST_ID, GAME_ID, List.of(
                assign(TEAM_A_ID, 2L, 3L, 5L), assign(TEAM_B_ID, 1L, 4L)));

        assertThat(m4.getTeam()).isSameAs(teamB);
        assertThat(m4.getPosX()).isEqualByComparingTo("70.00");
        assertThat(m4.getPosY()).isEqualByComparingTo("80.00");
        // 옮긴 선수도 배정 포지션은 바뀌지 않는다
        assertThat(m1.getPosition()).isEqualTo(Position.FW);
        assertThat(m3.getPosition()).isEqualTo(Position.MF);
        assertThat(lineup.getMethod()).isEqualTo(LineupMethod.ALGORITHM);
        assertThat(teamA.getFormation()).isNull();
    }

    @Test
    void 후보를_옮겨도_후보_여부는_그대로고_합계에는_더하지_않는다() {
        givenTeams();

        LineupDetail detail = lineupService.updateTeams(HOST_ID, GAME_ID, List.of(
                assign(TEAM_A_ID, 1L, 2L), assign(TEAM_B_ID, 3L, 4L, 5L)));

        assertThat(playerIds(detail.teams().get(1).members())).containsExactly(3L, 4L, 5L);
        assertThat(m5.getGamePlayer().isSubstitute()).isTrue();
        assertThat(teamA.getTotalSkill()).isEqualTo(15);
        assertThat(teamB.getTotalSkill()).isEqualTo(12);
    }

    @Test
    void 선발만_남은_팀과_후보만_남은_팀의_합계를_계산한다() {
        givenTeams();

        // A팀에는 후보 선수5만, B팀에는 나머지 전원
        lineupService.updateTeams(HOST_ID, GAME_ID, List.of(
                assign(TEAM_A_ID, 5L), assign(TEAM_B_ID, 1L, 2L, 3L, 4L)));

        assertThat(teamA.getTotalSkill()).isZero();
        assertThat(teamB.getTotalSkill()).isEqualTo(27);
    }

    @Test
    void 같은_요청을_다시_보내도_결과가_같고_좌표를_또_지우지_않는다() {
        givenTeams();
        List<TeamAssignment> request = List.of(assign(TEAM_A_ID, 2L, 3L, 5L), assign(TEAM_B_ID, 1L, 4L));

        LineupDetail first = lineupService.updateTeams(HOST_ID, GAME_ID, request);
        // 옮긴 뒤 파티장이 전술을 새로 배치했다고 하자
        m1.place(new BigDecimal("33.00"), new BigDecimal("44.00"));
        LineupDetail second = lineupService.updateTeams(HOST_ID, GAME_ID, request);

        assertThat(playerIds(second.teams().get(0).members())).isEqualTo(playerIds(first.teams().get(0).members()));
        assertThat(playerIds(second.teams().get(1).members())).isEqualTo(playerIds(first.teams().get(1).members()));
        assertThat(teamA.getTotalSkill()).isEqualTo(14);
        assertThat(teamB.getTotalSkill()).isEqualTo(13);
        // 아무도 옮기지 않았으므로 배치는 그대로다
        assertThat(m1.getPosX()).isEqualByComparingTo("33.00");
    }

    @Test
    void 아무도_옮기지_않는_요청도_받아들인다() {
        givenTeams();

        lineupService.updateTeams(HOST_ID, GAME_ID, List.of(
                assign(TEAM_A_ID, 1L, 2L, 5L), assign(TEAM_B_ID, 3L, 4L)));

        assertThat(m1.getPosX()).isEqualByComparingTo("10.00");
        assertThat(teamA.getTotalSkill()).isEqualTo(15);
        assertThat(teamB.getTotalSkill()).isEqualTo(12);
    }

    @Test
    void 요청의_팀_순서가_달라도_팀_ID로_맞춘다() {
        givenTeams();

        LineupDetail detail = lineupService.updateTeams(HOST_ID, GAME_ID, List.of(
                assign(TEAM_B_ID, 1L, 4L), assign(TEAM_A_ID, 2L, 3L, 5L)));

        // 응답은 요청 순서가 아니라 team_id 순이다
        assertThat(detail.teams().get(0).team()).isSameAs(teamA);
        assertThat(playerIds(detail.teams().get(0).members())).containsExactly(2L, 3L, 5L);
    }

    @Test
    void 없는_경기면_GAME_NOT_FOUND() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lineupService.updateTeams(HOST_ID, GAME_ID, List.of()))
                .hasFieldOrPropertyWithValue("errorCode", GameErrorCode.GAME_NOT_FOUND);
    }

    @Test
    void 삭제된_경기면_GAME_NOT_FOUND() {
        ReflectionTestUtils.setField(game, "deleted", true);
        givenGame();

        assertThatThrownBy(() -> lineupService.updateTeams(HOST_ID, GAME_ID, List.of()))
                .hasFieldOrPropertyWithValue("errorCode", GameErrorCode.GAME_NOT_FOUND);
    }

    @Test
    void 파티장이_아니면_NOT_GAME_HOST() {
        givenGame();

        assertThatThrownBy(() -> lineupService.updateTeams(OTHER_ID, GAME_ID, List.of()))
                .hasFieldOrPropertyWithValue("errorCode", GameErrorCode.NOT_GAME_HOST);
    }

    @Test
    void 라인업이_없으면_LINEUP_NOT_FOUND() {
        givenGame();
        when(lineupRepository.findFirstByGameIdOrderByVersionDesc(GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lineupService.updateTeams(HOST_ID, GAME_ID, List.of()))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.LINEUP_NOT_FOUND);
    }

    @Test
    void 확정된_라인업이면_LINEUP_CONFIRMED이고_아무것도_바꾸지_않는다() {
        lineup.confirm();
        givenLineup();

        assertThatThrownBy(() -> lineupService.updateTeams(HOST_ID, GAME_ID, List.of(
                assign(TEAM_A_ID, 2L, 3L, 5L), assign(TEAM_B_ID, 1L, 4L))))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.LINEUP_CONFIRMED);
        assertThat(m1.getTeam()).isSameAs(teamA);
    }

    @Test
    void 경기가_모집_중이_아니면_GAME_NOT_RECRUITING() {
        game.confirm();
        givenLineup();

        assertThatThrownBy(() -> lineupService.updateTeams(HOST_ID, GAME_ID, List.of(
                assign(TEAM_A_ID, 2L, 3L, 5L), assign(TEAM_B_ID, 1L, 4L))))
                .hasFieldOrPropertyWithValue("errorCode", GameErrorCode.GAME_NOT_RECRUITING);
        assertThat(m1.getTeam()).isSameAs(teamA);
    }

    @Test
    void 같은_선수가_두_팀에_있으면_DUPLICATE_PLACEMENT() {
        givenTeams();

        assertThatThrownBy(() -> lineupService.updateTeams(HOST_ID, GAME_ID, List.of(
                assign(TEAM_A_ID, 1L, 2L, 5L), assign(TEAM_B_ID, 1L, 3L, 4L))))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.DUPLICATE_PLACEMENT);
        assertThat(m1.getTeam()).isSameAs(teamA);
    }

    @Test
    void 같은_선수가_한_팀에_두_번_있어도_DUPLICATE_PLACEMENT() {
        givenTeams();

        assertThatThrownBy(() -> lineupService.updateTeams(HOST_ID, GAME_ID, List.of(
                assign(TEAM_A_ID, 1L, 1L, 2L, 5L), assign(TEAM_B_ID, 3L, 4L))))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.DUPLICATE_PLACEMENT);
    }

    @Test
    void 라인업에_없는_선수가_있으면_NOT_TEAM_MEMBER() {
        givenTeams();

        assertThatThrownBy(() -> lineupService.updateTeams(HOST_ID, GAME_ID, List.of(
                assign(TEAM_A_ID, 1L, 2L, 5L, 99L), assign(TEAM_B_ID, 3L, 4L))))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.NOT_TEAM_MEMBER);
    }

    @Test
    void 라인업의_선수가_빠지면_PLAYER_MISSING() {
        givenTeams();

        assertThatThrownBy(() -> lineupService.updateTeams(HOST_ID, GAME_ID, List.of(
                assign(TEAM_A_ID, 1L, 2L, 5L), assign(TEAM_B_ID, 3L))))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.PLAYER_MISSING);
        assertThat(teamA.getTotalSkill()).isEqualTo(15);
    }

    @Test
    void 팀에_선수가_없으면_EMPTY_TEAM이고_아무것도_바꾸지_않는다() {
        givenTeams();

        assertThatThrownBy(() -> lineupService.updateTeams(HOST_ID, GAME_ID, List.of(
                assign(TEAM_A_ID), assign(TEAM_B_ID, 1L, 2L, 3L, 4L, 5L))))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.EMPTY_TEAM);
        assertThat(m1.getTeam()).isSameAs(teamA);
        assertThat(m1.getPosX()).isEqualByComparingTo("10.00");
        assertThat(teamB.getTotalSkill()).isEqualTo(12);
    }

    @Test
    void 라인업의_팀이_빠지면_TEAM_SET_MISMATCH() {
        givenTeams();

        assertThatThrownBy(() -> lineupService.updateTeams(HOST_ID, GAME_ID, List.of(
                assign(TEAM_A_ID, 1L, 2L, 3L, 4L, 5L))))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.TEAM_SET_MISMATCH);
    }

    @Test
    void 요청에_라인업에_없는_팀이_있으면_TEAM_SET_MISMATCH() {
        givenTeams();

        assertThatThrownBy(() -> lineupService.updateTeams(HOST_ID, GAME_ID, List.of(
                assign(TEAM_A_ID, 1L, 2L, 5L), assign(999L, 3L, 4L))))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.TEAM_SET_MISMATCH);
    }

    @Test
    void 같은_팀이_두_번_오면_TEAM_SET_MISMATCH() {
        givenTeams();

        assertThatThrownBy(() -> lineupService.updateTeams(HOST_ID, GAME_ID, List.of(
                assign(TEAM_A_ID, 1L, 2L, 5L), assign(TEAM_A_ID, 3L, 4L))))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.TEAM_SET_MISMATCH);
    }

    @Test
    void 팀_목록이_비어_있으면_TEAM_SET_MISMATCH() {
        givenTeams();

        assertThatThrownBy(() -> lineupService.updateTeams(HOST_ID, GAME_ID, List.of()))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.TEAM_SET_MISMATCH);
    }
}
