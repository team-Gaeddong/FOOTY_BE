package com.gaeddong.footy.domain.lineup.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.domain.repository.PlayerPositionRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.lineup.domain.Lineup;
import com.gaeddong.footy.domain.lineup.domain.LineupMethod;
import com.gaeddong.footy.domain.lineup.domain.repository.LineupRepository;
import com.gaeddong.footy.domain.lineup.domain.repository.TeamMemberRepository;
import com.gaeddong.footy.domain.lineup.domain.repository.TeamRepository;
import com.gaeddong.footy.domain.lineup.exception.LineupErrorCode;
import com.gaeddong.footy.domain.lineup.service.LineupService.CreatedLineup;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LineupServiceTest {

    private static final Long HOST_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long GAME_ID = 10L;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 12, 0);

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

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL);
        lineupService = new LineupService(gameRepository, gamePlayerRepository, playerPositionRepository,
                lineupRepository, teamRepository, teamMemberRepository, clock);
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

    private static Game game() {
        return game(12);
    }

    private static Game game(int maxPlayers) {
        Game game = Game.create(member(HOST_ID), "일요일 풋살", NOW.plusDays(3), "대구 풋살장", maxPlayers, 2, NOW);
        ReflectionTestUtils.setField(game, "id", GAME_ID);
        return game;
    }

    private static GamePlayer player(Game game, Long id, Integer skillLevel) {
        GamePlayer player = new GamePlayer(game, null, "선수" + id, NOW.plusMinutes(id));
        ReflectionTestUtils.setField(player, "id", id);
        if (skillLevel != null) {
            player.changeSkillLevel(skillLevel);
        }
        return player;
    }

    private static PlayerPosition position(GamePlayer player, Position position, int priority) {
        return PlayerPosition.builder().gamePlayer(player).position(position).priority(priority).build();
    }

    private void givenGame(Game game, List<GamePlayer> players) {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(lineupRepository.existsByGameId(GAME_ID)).thenReturn(false);
        when(gamePlayerRepository.findAllByGameIdOrderByJoinedAtAscIdAsc(GAME_ID)).thenReturn(players);
    }

    private void givenSavesPassThrough() {
        when(lineupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(teamRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(teamMemberRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void 실력_높은_순으로_두_팀에_번갈아_배정한다() {
        Game game = game();
        // 참여 순서와 실력 순서를 일부러 다르게 둔다
        GamePlayer p1 = player(game, 1L, 5);
        GamePlayer p2 = player(game, 2L, 9);
        GamePlayer p3 = player(game, 3L, 7);
        GamePlayer p4 = player(game, 4L, 3);
        GamePlayer p5 = player(game, 5L, 8);
        givenGame(game, List.of(p1, p2, p3, p4, p5));
        when(playerPositionRepository.findAllByGamePlayerIdInOrderByPriorityAsc(anyList())).thenReturn(List.of());
        givenSavesPassThrough();

        CreatedLineup created = lineupService.create(HOST_ID, GAME_ID);

        // 9(p2) → A, 8(p5) → B, 7(p3) → A, 5(p1) → B, 3(p4) → A
        assertThat(created.teams()).hasSize(2);
        assertThat(created.teams().get(0).team().getName()).isEqualTo("A팀");
        assertThat(created.teams().get(0).members())
                .extracting(member -> member.getGamePlayer().getId())
                .containsExactly(2L, 3L, 4L);
        assertThat(created.teams().get(0).team().getTotalSkill()).isEqualTo(19);
        assertThat(created.teams().get(1).team().getName()).isEqualTo("B팀");
        assertThat(created.teams().get(1).members())
                .extracting(member -> member.getGamePlayer().getId())
                .containsExactly(5L, 1L);
        assertThat(created.teams().get(1).team().getTotalSkill()).isEqualTo(13);
    }

    @Test
    void 라인업은_1회차_알고리즘_미확정으로_만든다() {
        Game game = game();
        givenGame(game, List.of(player(game, 1L, 5), player(game, 2L, 6)));
        when(playerPositionRepository.findAllByGamePlayerIdInOrderByPriorityAsc(anyList())).thenReturn(List.of());
        givenSavesPassThrough();

        Lineup lineup = lineupService.create(HOST_ID, GAME_ID).lineup();

        assertThat(lineup.getGame()).isSameAs(game);
        assertThat(lineup.getVersion()).isEqualTo(1);
        assertThat(lineup.getMethod()).isEqualTo(LineupMethod.ALGORITHM);
        assertThat(lineup.isConfirmed()).isFalse();
        assertThat(lineup.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void 실력이_같으면_먼저_참여한_선수가_앞선다() {
        Game game = game();
        GamePlayer early = player(game, 1L, 7);
        GamePlayer late = player(game, 2L, 7);
        givenGame(game, List.of(early, late));
        when(playerPositionRepository.findAllByGamePlayerIdInOrderByPriorityAsc(anyList())).thenReturn(List.of());
        givenSavesPassThrough();

        CreatedLineup created = lineupService.create(HOST_ID, GAME_ID);

        assertThat(created.teams().get(0).members().get(0).getGamePlayer()).isSameAs(early);
        assertThat(created.teams().get(1).members().get(0).getGamePlayer()).isSameAs(late);
    }

    @Test
    void 배정_포지션은_1순위이고_없으면_FW() {
        Game game = game();
        GamePlayer withPositions = player(game, 1L, 9);
        GamePlayer withoutPositions = player(game, 2L, 5);
        givenGame(game, List.of(withPositions, withoutPositions));
        when(playerPositionRepository.findAllByGamePlayerIdInOrderByPriorityAsc(List.of(1L, 2L)))
                .thenReturn(List.of(position(withPositions, Position.GK, 1), position(withPositions, Position.DF, 2)));
        givenSavesPassThrough();

        CreatedLineup created = lineupService.create(HOST_ID, GAME_ID);

        assertThat(created.teams())
                .flatExtracting(LineupService.CreatedTeam::members)
                .extracting(member -> member.getGamePlayer().getId(), member -> member.getPosition())
                .containsExactly(tuple(1L, Position.GK), tuple(2L, Position.FW));
    }

    @Test
    void 최대_인원_이하면_후보가_없다() {
        Game game = game(2);
        GamePlayer p1 = player(game, 1L, 5);
        GamePlayer p2 = player(game, 2L, 6);
        givenGame(game, List.of(p1, p2));
        when(playerPositionRepository.findAllByGamePlayerIdInOrderByPriorityAsc(anyList())).thenReturn(List.of());
        givenSavesPassThrough();

        lineupService.create(HOST_ID, GAME_ID);

        assertThat(List.of(p1, p2)).noneMatch(GamePlayer::isSubstitute);
    }

    @Test
    void 최대_인원을_넘으면_실력이_낮은_선수가_후보가_되고_합계에서_빠진다() {
        Game game = game(3);
        GamePlayer p1 = player(game, 1L, 5);
        GamePlayer p2 = player(game, 2L, 9);
        GamePlayer p3 = player(game, 3L, 7);
        GamePlayer p4 = player(game, 4L, 3);
        GamePlayer p5 = player(game, 5L, 8);
        givenGame(game, List.of(p1, p2, p3, p4, p5));
        when(playerPositionRepository.findAllByGamePlayerIdInOrderByPriorityAsc(anyList())).thenReturn(List.of());
        givenSavesPassThrough();

        CreatedLineup created = lineupService.create(HOST_ID, GAME_ID);

        // 선발 9(p2), 8(p5), 7(p3) / 후보 5(p1), 3(p4)
        assertThat(List.of(p2, p5, p3)).noneMatch(GamePlayer::isSubstitute);
        assertThat(List.of(p1, p4)).allMatch(GamePlayer::isSubstitute);
        // 후보도 번갈아 배정한다: A = p2, p3, p4(후보), B = p5, p1(후보)
        assertThat(created.teams().get(0).members())
                .extracting(member -> member.getGamePlayer().getId())
                .containsExactly(2L, 3L, 4L);
        assertThat(created.teams().get(0).team().getTotalSkill()).isEqualTo(16);
        assertThat(created.teams().get(1).members())
                .extracting(member -> member.getGamePlayer().getId())
                .containsExactly(5L, 1L);
        assertThat(created.teams().get(1).team().getTotalSkill()).isEqualTo(8);
    }

    @Test
    void 후보_경계에서_실력이_같으면_늦게_참여한_선수가_후보가_된다() {
        Game game = game(2);
        GamePlayer top = player(game, 1L, 9);
        GamePlayer early = player(game, 2L, 5);
        GamePlayer late = player(game, 3L, 5);
        givenGame(game, List.of(top, early, late));
        when(playerPositionRepository.findAllByGamePlayerIdInOrderByPriorityAsc(anyList())).thenReturn(List.of());
        givenSavesPassThrough();

        lineupService.create(HOST_ID, GAME_ID);

        assertThat(early.isSubstitute()).isFalse();
        assertThat(late.isSubstitute()).isTrue();
    }

    @Test
    void 없는_경기면_GAME_NOT_FOUND() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lineupService.create(HOST_ID, GAME_ID))
                .hasFieldOrPropertyWithValue("errorCode", GameErrorCode.GAME_NOT_FOUND);
    }

    @Test
    void 삭제된_경기면_GAME_NOT_FOUND() {
        Game game = game();
        ReflectionTestUtils.setField(game, "deleted", true);
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> lineupService.create(HOST_ID, GAME_ID))
                .hasFieldOrPropertyWithValue("errorCode", GameErrorCode.GAME_NOT_FOUND);
    }

    @Test
    void 파티장이_아니면_NOT_GAME_HOST() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));

        assertThatThrownBy(() -> lineupService.create(OTHER_ID, GAME_ID))
                .hasFieldOrPropertyWithValue("errorCode", GameErrorCode.NOT_GAME_HOST);
        verify(lineupRepository, never()).save(any());
    }

    @Test
    void 이미_라인업이_있으면_LINEUP_ALREADY_EXISTS() {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
        when(lineupRepository.existsByGameId(GAME_ID)).thenReturn(true);

        assertThatThrownBy(() -> lineupService.create(HOST_ID, GAME_ID))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.LINEUP_ALREADY_EXISTS);
        verify(lineupRepository, never()).save(any());
    }

    @Test
    void 선수가_2명_미만이면_NOT_ENOUGH_PLAYERS() {
        Game game = game();
        givenGame(game, List.of(player(game, 1L, 5)));

        assertThatThrownBy(() -> lineupService.create(HOST_ID, GAME_ID))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.NOT_ENOUGH_PLAYERS);
        verify(lineupRepository, never()).save(any());
    }

    @Test
    void 실력이_없는_선수가_있으면_UNRATED_PLAYER_EXISTS() {
        Game game = game();
        givenGame(game, List.of(player(game, 1L, 5), player(game, 2L, null)));

        assertThatThrownBy(() -> lineupService.create(HOST_ID, GAME_ID))
                .hasFieldOrPropertyWithValue("errorCode", LineupErrorCode.UNRATED_PLAYER_EXISTS);
        verify(lineupRepository, never()).save(any());
    }
}
