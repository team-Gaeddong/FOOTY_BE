package com.gaeddong.footy.domain.lineup.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.lineup.domain.Team;
import com.gaeddong.footy.domain.lineup.domain.TeamMember;
import com.gaeddong.footy.domain.lineup.domain.repository.TeamMemberRepository;
import com.gaeddong.footy.domain.lineup.domain.repository.TeamRepository;
import com.gaeddong.footy.domain.lineup.exception.LineupErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TacticService {

    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * 팀의 전술 배치도를 조회한다. 로그인한 회원 누구나 볼 수 있다(선수 목록 D-20과 같은 기준).
     */
    @Transactional(readOnly = true)
    public Tactic get(Long gameId, Long teamId) {
        findGame(gameId);
        Team team = findTeam(gameId, teamId);
        return new Tactic(team, teamMemberRepository.findAllByTeamIdOrderByIdAsc(teamId));
    }

    /**
     * 포메이션과 배치를 통째로 바꾼다(#45). placements에 없는 팀원은 배치 해제된다.
     * 파티장만 할 수 있고, 팀이 확정된 뒤에는 바꿀 수 없다.
     */
    @Transactional
    public Tactic update(Long requesterId, Long gameId, Long teamId, String formation,
                         Map<Long, Coordinate> placements) {
        Game game = findGame(gameId);
        if (!game.getHost().getId().equals(requesterId)) {
            throw new FootyException(GameErrorCode.NOT_GAME_HOST);
        }
        Team team = findTeam(gameId, teamId);
        if (team.getLineup().isConfirmed()) {
            throw new FootyException(LineupErrorCode.LINEUP_CONFIRMED);
        }

        List<TeamMember> members = teamMemberRepository.findAllByTeamIdOrderByIdAsc(teamId);
        Set<Long> memberPlayerIds = members.stream()
                .map(member -> member.getGamePlayer().getId())
                .collect(Collectors.toSet());
        if (!memberPlayerIds.containsAll(placements.keySet())) {
            throw new FootyException(LineupErrorCode.NOT_TEAM_MEMBER);
        }

        team.changeFormation(formation);
        for (TeamMember member : members) {
            Coordinate coordinate = placements.get(member.getGamePlayer().getId());
            if (coordinate == null) {
                member.unplace();
            } else {
                member.place(coordinate.posX(), coordinate.posY());
            }
        }
        return new Tactic(team, members);
    }

    private Game findGame(Long gameId) {
        return gameRepository.findById(gameId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new FootyException(GameErrorCode.GAME_NOT_FOUND));
    }

    private Team findTeam(Long gameId, Long teamId) {
        return teamRepository.findByIdAndLineupGameId(teamId, gameId)
                .orElseThrow(() -> new FootyException(LineupErrorCode.TEAM_NOT_FOUND));
    }

    public record Coordinate(BigDecimal posX, BigDecimal posY) {
    }

    public record Tactic(Team team, List<TeamMember> members) {
    }
}
