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

    private static final List<String> TEAM_NAMES = List.of("A팀", "B팀");
    private static final Position DEFAULT_POSITION = Position.FW;

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final PlayerPositionRepository playerPositionRepository;
    private final LineupRepository lineupRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final Clock clock;

    /**
     * 실력 점수가 높은 선수부터 두 팀에 한 명씩 번갈아 배정한다(#40).
     * 실력이 같으면 먼저 참여한 선수가 앞선다. 배정 포지션은 1순위 희망 포지션이고, 없으면 FW다.
     */
    @Transactional
    public CreatedLineup create(Long requesterId, Long gameId) {
        Game game = gameRepository.findById(gameId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new FootyException(GameErrorCode.GAME_NOT_FOUND));
        if (!game.getHost().getId().equals(requesterId)) {
            throw new FootyException(GameErrorCode.NOT_GAME_HOST);
        }
        // 재배정은 다시 짜기(lineup.version 증가)에서 한다
        if (lineupRepository.existsByGameId(gameId)) {
            throw new FootyException(LineupErrorCode.LINEUP_ALREADY_EXISTS);
        }

        List<GamePlayer> players = gamePlayerRepository.findAllByGameIdOrderByJoinedAtAscIdAsc(gameId);
        if (players.size() < TEAM_NAMES.size()) {
            throw new FootyException(LineupErrorCode.NOT_ENOUGH_PLAYERS);
        }
        if (players.stream().anyMatch(player -> player.getSkillLevel() == null)) {
            throw new FootyException(LineupErrorCode.UNRATED_PLAYER_EXISTS);
        }

        // 정렬은 안정적이라 실력이 같으면 참여 순서가 유지된다
        List<GamePlayer> ranked = players.stream()
                .sorted(Comparator.comparing(GamePlayer::getSkillLevel).reversed())
                .toList();
        List<List<GamePlayer>> assigned = new ArrayList<>();
        TEAM_NAMES.forEach(name -> assigned.add(new ArrayList<>()));
        for (int i = 0; i < ranked.size(); i++) {
            assigned.get(i % TEAM_NAMES.size()).add(ranked.get(i));
        }

        Map<Long, Position> firstPositions = firstPositions(players);
        Lineup lineup = lineupRepository.save(Lineup.builder()
                .game(game)
                .version(1)
                .method(LineupMethod.ALGORITHM)
                .createdAt(LocalDateTime.now(clock))
                .build());

        List<CreatedTeam> teams = new ArrayList<>();
        for (int i = 0; i < TEAM_NAMES.size(); i++) {
            List<GamePlayer> teamPlayers = assigned.get(i);
            Team team = teamRepository.save(Team.builder()
                    .lineup(lineup)
                    .name(TEAM_NAMES.get(i))
                    .totalSkill(teamPlayers.stream().mapToInt(GamePlayer::getSkillLevel).sum())
                    .build());
            List<TeamMember> members = teamMemberRepository.saveAll(teamPlayers.stream()
                    .map(player -> TeamMember.builder()
                            .team(team)
                            .gamePlayer(player)
                            .position(firstPositions.getOrDefault(player.getId(), DEFAULT_POSITION))
                            .build())
                    .toList());
            teams.add(new CreatedTeam(team, members));
        }
        return new CreatedLineup(lineup, teams);
    }

    // 선수마다 따로 조회하지 않도록 포지션을 한 번에 가져와 1순위만 남긴다
    private Map<Long, Position> firstPositions(List<GamePlayer> players) {
        return playerPositionRepository
                .findAllByGamePlayerIdInOrderByPriorityAsc(players.stream().map(GamePlayer::getId).toList())
                .stream()
                .collect(Collectors.toMap(position -> position.getGamePlayer().getId(),
                        PlayerPosition::getPosition, (first, ignored) -> first));
    }

    public record CreatedLineup(Lineup lineup, List<CreatedTeam> teams) {
    }

    public record CreatedTeam(Team team, List<TeamMember> members) {
    }
}
