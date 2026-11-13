package com.gaeddong.footy.domain.lineup.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
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
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LineupService {

    // 팀은 항상 2개다. game.team_count는 아직 쓰지 않는다(D-21)
    private static final List<String> TEAM_NAMES = List.of("A팀", "B팀");
    private static final int FIRST_VERSION = 1;
    private static final Position DEFAULT_POSITION = Position.FW;

    private final GameReader gameReader;
    private final GamePlayerRepository gamePlayerRepository;
    private final PlayerPositionRepository playerPositionRepository;
    private final LineupRepository lineupRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final Clock clock;

    /**
     * 실력 점수가 높은 선수부터 두 팀에 한 명씩 번갈아 배정한다(#40).
     * 실력이 같으면 먼저 참여한 선수가 앞선다. 배정 포지션은 1순위 희망 포지션이고, 없으면 FW다.
     * 최대 인원을 넘는 뒤쪽 선수는 후보로 지정하고, 팀에는 배정하되 실력 합계에서 뺀다(#42).
     */
    @Transactional
    public LineupDetail create(Long requesterId, Long gameId) {
        Game game = gameReader.getGameAsHost(gameId, requesterId);
        // 재배정은 다시 짜기(lineup.version 증가)에서 한다
        if (lineupRepository.existsByGameId(gameId)) {
            throw new FootyException(LineupErrorCode.LINEUP_ALREADY_EXISTS);
        }

        List<GamePlayer> players = findAssignablePlayers(gameId);
        List<GamePlayer> ranked = rankBySkill(players);
        markSubstitutes(ranked, game.getMaxPlayers());
        List<List<GamePlayer>> rosters = dealInTurn(ranked);
        Map<Long, Position> firstPositions = findFirstPositions(players);

        Lineup lineup = lineupRepository.save(Lineup.builder()
                .game(game)
                .version(FIRST_VERSION)
                .method(LineupMethod.ALGORITHM)
                .createdAt(LocalDateTime.now(clock))
                .build());
        List<TeamDetail> teams = new ArrayList<>();
        for (int i = 0; i < TEAM_NAMES.size(); i++) {
            teams.add(saveTeam(lineup, TEAM_NAMES.get(i), rosters.get(i), firstPositions));
        }
        return new LineupDetail(lineup, teams);
    }

    /**
     * 가장 최근 회차(version)의 라인업을 조회한다(#49). 로그인한 회원 누구나 볼 수 있고,
     * 실력은 파티장에게만 보인다(D-20과 같은 기준, D-24).
     */
    @Transactional(readOnly = true)
    public ViewedLineup getLatest(Long requesterId, Long gameId) {
        Game game = gameReader.getGame(gameId);
        Lineup lineup = getLatestLineup(gameId);
        return new ViewedLineup(new LineupDetail(lineup, findTeams(lineup)), game.isHostedBy(requesterId));
    }

    /**
     * 가장 최근 회차의 라인업과 경기를 확정한다(#48). 파티장만 할 수 있다.
     * 종료된 경기만 막으므로, 이미 확정한 경기에 다시 요청하면 같은 결과를 돌려준다.
     */
    @Transactional
    public LineupDetail confirm(Long requesterId, Long gameId) {
        Game game = gameReader.getGameAsHost(gameId, requesterId);
        if (game.isFinished()) {
            throw new FootyException(GameErrorCode.GAME_NOT_RECRUITING);
        }

        Lineup lineup = getLatestLineup(gameId);
        lineup.confirm();
        game.confirm();
        return new LineupDetail(lineup, findTeams(lineup));
    }

    // 참여 순으로 가져온다. 선수가 팀 수보다 적거나 실력이 비어 있는 선수가 있으면 팀을 짤 수 없다
    private List<GamePlayer> findAssignablePlayers(Long gameId) {
        List<GamePlayer> players = gamePlayerRepository.findAllByGameIdOrderByJoinedAtAscIdAsc(gameId);
        if (players.size() < TEAM_NAMES.size()) {
            throw new FootyException(LineupErrorCode.NOT_ENOUGH_PLAYERS);
        }
        if (players.stream().anyMatch(player -> player.getSkillLevel() == null)) {
            throw new FootyException(LineupErrorCode.UNRATED_PLAYER_EXISTS);
        }
        return players;
    }

    // 정렬은 안정적이라 실력이 같으면 참여 순서가 유지된다
    private List<GamePlayer> rankBySkill(List<GamePlayer> playersInJoinOrder) {
        return playersInJoinOrder.stream()
                .sorted(Comparator.comparing(GamePlayer::getSkillLevel).reversed())
                .toList();
    }

    // 앞의 maxPlayers명이 선발, 나머지가 후보다(D-22)
    private void markSubstitutes(List<GamePlayer> ranked, int maxPlayers) {
        for (int rank = 0; rank < ranked.size(); rank++) {
            ranked.get(rank).changeSubstitute(rank >= maxPlayers);
        }
    }

    // 순위대로 A팀, B팀, A팀, ... 순서로 한 명씩 나눈다. 후보도 같은 방식으로 나눈다
    private List<List<GamePlayer>> dealInTurn(List<GamePlayer> ranked) {
        List<List<GamePlayer>> rosters = new ArrayList<>();
        TEAM_NAMES.forEach(name -> rosters.add(new ArrayList<>()));
        for (int rank = 0; rank < ranked.size(); rank++) {
            rosters.get(rank % rosters.size()).add(ranked.get(rank));
        }
        return rosters;
    }

    private TeamDetail saveTeam(Lineup lineup, String name, List<GamePlayer> roster,
                                Map<Long, Position> firstPositions) {
        Team team = teamRepository.save(Team.builder()
                .lineup(lineup)
                .name(name)
                .totalSkill(sumStarterSkill(roster))
                .build());
        List<TeamMember> members = teamMemberRepository.saveAll(roster.stream()
                .map(player -> TeamMember.builder()
                        .team(team)
                        .gamePlayer(player)
                        .position(firstPositions.getOrDefault(player.getId(), DEFAULT_POSITION))
                        .build())
                .toList());
        return new TeamDetail(team, members);
    }

    // 후보는 팀 실력 합계에서 뺀다(D-22)
    private int sumStarterSkill(List<GamePlayer> roster) {
        return roster.stream()
                .filter(player -> !player.isSubstitute())
                .mapToInt(GamePlayer::getSkillLevel)
                .sum();
    }

    // 선수마다 따로 조회하지 않도록 포지션을 한 번에 가져와 1순위만 남긴다
    private Map<Long, Position> findFirstPositions(List<GamePlayer> players) {
        return playerPositionRepository
                .findAllByGamePlayerIdInOrderByPriorityAsc(players.stream().map(GamePlayer::getId).toList())
                .stream()
                .collect(Collectors.toMap(position -> position.getGamePlayer().getId(),
                        PlayerPosition::getPosition, (first, ignored) -> first));
    }

    private Lineup getLatestLineup(Long gameId) {
        return lineupRepository.findFirstByGameIdOrderByVersionDesc(gameId)
                .orElseThrow(() -> new FootyException(LineupErrorCode.LINEUP_NOT_FOUND));
    }

    // 팀은 team_id 순, 팀원은 배정 순(team_member_id)이다. 팀마다 따로 조회하지 않도록 팀원을 한 번에 가져온다
    private List<TeamDetail> findTeams(Lineup lineup) {
        List<Team> teams = teamRepository.findAllByLineupIdOrderByIdAsc(lineup.getId());
        Map<Long, List<TeamMember>> membersByTeamId = teamMemberRepository
                .findAllByTeamIdInOrderByIdAsc(teams.stream().map(Team::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(member -> member.getTeam().getId()));
        return teams.stream()
                .map(team -> new TeamDetail(team, membersByTeamId.getOrDefault(team.getId(), List.of())))
                .toList();
    }

    public record LineupDetail(Lineup lineup, List<TeamDetail> teams) {
    }

    public record TeamDetail(Team team, List<TeamMember> members) {
    }

    /**
     * skillVisible은 요청자에게 실력을 보여줄지다. 파티장이 아니면 false.
     */
    public record ViewedLineup(LineupDetail lineup, boolean skillVisible) {
    }
}
