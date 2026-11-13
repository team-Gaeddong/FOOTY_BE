package com.gaeddong.footy.domain.lineup.service;

import com.gaeddong.footy.domain.game.service.GameReader;
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

    private final GameReader gameReader;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * 팀의 전술 배치도를 조회한다. 로그인한 회원 누구나 볼 수 있다(선수 목록 D-20과 같은 기준).
     */
    @Transactional(readOnly = true)
    public Tactic get(Long gameId, Long teamId) {
        // 삭제된 경기의 팀은 보여주지 않는다
        gameReader.getGame(gameId);
        Team team = findTeam(gameId, teamId);
        return new Tactic(team, teamMemberRepository.findAllByTeamIdOrderByIdAsc(teamId));
    }

    /**
     * 포메이션과 배치를 통째로 바꾼다(#45). placements에 없는 팀원은 배치 해제된다.
     * 파티장만 할 수 있고, 팀이 확정된 뒤에는 바꿀 수 없다.
     *
     * @param placements 경기 참가자 ID(game_player_id)별 좌표
     */
    @Transactional
    public Tactic update(Long requesterId, Long gameId, Long teamId, String formation,
                         Map<Long, Coordinate> placements) {
        gameReader.getGameAsHost(gameId, requesterId);
        Team team = findTeam(gameId, teamId);
        if (team.getLineup().isConfirmed()) {
            throw new FootyException(LineupErrorCode.LINEUP_CONFIRMED);
        }

        List<TeamMember> members = teamMemberRepository.findAllByTeamIdOrderByIdAsc(teamId);
        validateAllInTeam(placements.keySet(), members);

        team.changeFormation(formation);
        members.forEach(member -> applyPlacement(member, placements.get(member.getGamePlayer().getId())));
        return new Tactic(team, members);
    }

    private void validateAllInTeam(Set<Long> placedPlayerIds, List<TeamMember> members) {
        Set<Long> memberPlayerIds = members.stream()
                .map(member -> member.getGamePlayer().getId())
                .collect(Collectors.toSet());
        if (!memberPlayerIds.containsAll(placedPlayerIds)) {
            throw new FootyException(LineupErrorCode.NOT_TEAM_MEMBER);
        }
    }

    // 좌표가 없으면(요청 목록에 없는 팀원) 배치를 해제한다
    private void applyPlacement(TeamMember member, Coordinate coordinate) {
        if (coordinate == null) {
            member.unplace();
        } else {
            member.place(coordinate.posX(), coordinate.posY());
        }
    }

    // 다른 경기에 속한 팀도 없는 팀으로 본다
    private Team findTeam(Long gameId, Long teamId) {
        return teamRepository.findByIdAndLineupGameId(teamId, gameId)
                .orElseThrow(() -> new FootyException(LineupErrorCode.TEAM_NOT_FOUND));
    }

    public record Coordinate(BigDecimal posX, BigDecimal posY) {
    }

    public record Tactic(Team team, List<TeamMember> members) {
    }
}
