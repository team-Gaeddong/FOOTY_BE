package com.gaeddong.footy.domain.lineup.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.lineup.domain.Lineup;
import com.gaeddong.footy.domain.lineup.domain.LineupMethod;
import com.gaeddong.footy.domain.lineup.domain.Team;
import com.gaeddong.footy.domain.lineup.domain.TeamMember;
import com.gaeddong.footy.domain.lineup.domain.repository.TeamMemberRepository;
import com.gaeddong.footy.domain.lineup.domain.repository.TeamRepository;
import com.gaeddong.footy.domain.lineup.exception.LineupErrorCode;
import com.gaeddong.footy.domain.lineup.service.TacticService.Coordinate;
import com.gaeddong.footy.domain.lineup.service.TacticService.Tactic;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TacticServiceTest {

    private static final Long HOST_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long GAME_ID = 10L;
    private static final Long TEAM_ID = 100L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 12, 0);

    @Mock
    private GameRepository gameRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    private TacticService tacticService;

    private Game game;
    private Team team;
    private TeamMember m1;
    private TeamMember m2;

    @BeforeEach
    void setUp() {
        tacticService = new TacticService(gameRepository, teamRepository, teamMemberRepository);
        game = Game.create(member(HOST_ID), "일요일 풋살", NOW.plusDays(3), "대구 풋살장", 12, 2, NOW);
        ReflectionTestUtils.setField(game, "id", GAME_ID);
        team = team(false);
        m1 = teamMember(player(1L), Position.FW);
        m2 = teamMember(player(2L), Position.GK);
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

    private GamePlayer player(Long id) {
        GamePlayer player = new GamePlayer(game, null, "선수" + id, NOW.plusMinutes(id));
        ReflectionTestUtils.setField(player, "id", id);
        return player;
    }

    private Team team(boolean confirmed) {
        Lineup lineup = Lineup.builder()
                .game(game)
                .version(1)
                .method(LineupMethod.ALGORITHM)
                .confirmed(confirmed)
                .createdAt(NOW)
                .build();
        Team created = Team.builder().lineup(lineup).name("A팀").build();
        ReflectionTestUtils.setField(created, "id", TEAM_ID);
        return created;
    }

    private TeamMember teamMember(GamePlayer player, Position position) {
        return TeamMember.builder().team(team).gamePlayer(player).position(position).build();
    }

    private void givenTeam() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(teamRepository.findByIdAndLineupGameId(TEAM_ID, GAME_ID)).thenReturn(Optional.of(team));
    }

    private static Coordinate coordinate(String posX, String posY) {
        return new Coordinate(new BigDecimal(posX), new BigDecimal(posY));
    }

    @Test
    void 포메이션과_배치를_저장한다() {
        givenTeam();
        when(teamMemberRepository.findAllByTeamIdOrderByIdAsc(TEAM_ID)).thenReturn(List.of(m1, m2));

        Tactic tactic = tacticService.update(HOST_ID, GAME_ID, TEAM_ID, "4-4-2",
                Map.of(1L, coordinate("50.00", "20.5"), 2L, coordinate("50", "95.25")));

        assertThat(tactic.team().getFormation()).isEqualTo("4-4-2");
        assertThat(tactic.members())
                .extracting(member -> member.getGamePlayer().getId(), TeamMember::getPosX, TeamMember::getPosY)
                .containsExactly(
                        tuple(1L, new BigDecimal("50.00"), new BigDecimal("20.5")),
                        tuple(2L, new BigDecimal("50"), new BigDecimal("95.25")));
    }

    @Test
    void 목록에_없는_팀원은_배치_해제한다() {
        givenTeam();
        m2.place(new BigDecimal("10"), new BigDecimal("10"));
        when(teamMemberRepository.findAllByTeamIdOrderByIdAsc(TEAM_ID)).thenReturn(List.of(m1, m2));

        tacticService.update(HOST_ID, GAME_ID, TEAM_ID, null, Map.of(1L, coordinate("0", "100")));

        assertThat(m1.getPosX()).isEqualByComparingTo("0");
        assertThat(m1.getPosY()).isEqualByComparingTo("100");
        assertThat(m2.getPosX()).isNull();
        assertThat(m2.getPosY()).isNull();
    }

    @Test
    void 배치는_배정_포지션을_바꾸지_않는다() {
        givenTeam();
        when(teamMemberRepository.findAllByTeamIdOrderByIdAsc(TEAM_ID)).thenReturn(List.of(m1, m2));

        tacticService.update(HOST_ID, GAME_ID, TEAM_ID, "4-3-3", Map.of(2L, coordinate("50", "50")));

        assertThat(m2.getPosition()).isEqualTo(Position.GK);
    }

    @Test
    void 조회하면_팀원_배치를_반환한다() {
        givenTeam();
        m1.place(new BigDecimal("30"), new BigDecimal("40"));
        when(teamMemberRepository.findAllByTeamIdOrderByIdAsc(TEAM_ID)).thenReturn(List.of(m1, m2));

        Tactic tactic = tacticService.get(GAME_ID, TEAM_ID);

        assertThat(tactic.team()).isSameAs(team);
        assertThat(tactic.members()).containsExactly(m1, m2);
    }

    @Test
    void 없는_경기면_GAME_NOT_FOUND() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tacticService.update(HOST_ID, GAME_ID, TEAM_ID, null, Map.of()))
                .hasFieldOrPropertyWithValue("errorCode", GameErrorCode.GAME_NOT_FOUND);
        assertThatThrownBy(() -> tacticService.get(GAME_ID, TEAM_ID))
                .hasFieldOrPropertyWithValue("errorCode", GameErrorCode.GAME_NOT_FOUND);
    }

    @Test
    void 삭제된_경기면_GAME_NOT_FOUND() {
        ReflectionTestUtils.setField(game, "deleted", true);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> tacticService.update(HOST_ID, GAME_ID, TEAM_ID, null, Map.of()))
                .hasFieldOrPropertyWithValue("errorCode", GameErrorCode.GAME_NOT_FOUND);
    }

    @Test
    void 파티장이_아니면_NOT_GAME_HOST() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> tacticService.update(OTHER_ID, GAME_ID, TEAM_ID, null, Map.of()))
                .hasFieldOrPropertyWithValue("errorCode", GameErrorCode.NOT_GAME_HOST);
        verify(teamRepository, never()).findByIdAndLineupGameId(anyLong(), anyLong());
    }

    @Test
    void 경기에_속한_팀이_아니면_TEAM_NOT_FOUND() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(teamRepository.findByIdAndLineupGameId(TEAM_ID, GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tacticService.update(HOST_ID, GAME_ID, TEAM_ID, null, Map.of()))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.TEAM_NOT_FOUND);
        assertThatThrownBy(() -> tacticService.get(GAME_ID, TEAM_ID))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.TEAM_NOT_FOUND);
    }

    @Test
    void 확정된_팀이면_LINEUP_CONFIRMED() {
        team = team(true);
        givenTeam();

        assertThatThrownBy(() -> tacticService.update(HOST_ID, GAME_ID, TEAM_ID, null, Map.of()))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.LINEUP_CONFIRMED);
        verify(teamMemberRepository, never()).findAllByTeamIdOrderByIdAsc(anyLong());
    }

    @Test
    void 팀원이_아닌_선수를_배치하면_NOT_TEAM_MEMBER_이고_아무것도_바꾸지_않는다() {
        givenTeam();
        when(teamMemberRepository.findAllByTeamIdOrderByIdAsc(TEAM_ID)).thenReturn(List.of(m1, m2));

        assertThatThrownBy(() -> tacticService.update(HOST_ID, GAME_ID, TEAM_ID, "4-4-2",
                Map.of(1L, coordinate("10", "10"), 99L, coordinate("20", "20"))))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.NOT_TEAM_MEMBER);
        assertThat(team.getFormation()).isNull();
        assertThat(m1.getPosX()).isNull();
    }
}
