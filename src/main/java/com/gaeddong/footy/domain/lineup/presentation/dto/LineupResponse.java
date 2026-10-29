package com.gaeddong.footy.domain.lineup.presentation.dto;

import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.lineup.domain.Lineup;
import com.gaeddong.footy.domain.lineup.domain.LineupMethod;
import com.gaeddong.footy.domain.lineup.domain.Team;
import com.gaeddong.footy.domain.lineup.domain.TeamMember;
import com.gaeddong.footy.domain.lineup.service.LineupService.CreatedLineup;
import com.gaeddong.footy.domain.lineup.service.LineupService.CreatedTeam;
import com.gaeddong.footy.domain.member.domain.Position;

import java.util.List;

public record LineupResponse(
        Long lineupId,
        int version,
        LineupMethod method,
        boolean isConfirmed,
        List<TeamResponse> teams
) {
    public static LineupResponse from(CreatedLineup created) {
        Lineup lineup = created.lineup();
        return new LineupResponse(
                lineup.getId(),
                lineup.getVersion(),
                lineup.getMethod(),
                lineup.isConfirmed(),
                created.teams().stream().map(TeamResponse::from).toList()
        );
    }

    public record TeamResponse(Long teamId, String name, Integer totalSkill, List<TeamMemberResponse> members) {
        static TeamResponse from(CreatedTeam created) {
            Team team = created.team();
            return new TeamResponse(
                    team.getId(),
                    team.getName(),
                    team.getTotalSkill(),
                    created.members().stream().map(TeamMemberResponse::from).toList()
            );
        }
    }

    public record TeamMemberResponse(Long gamePlayerId, String name, Integer skillLevel, Position position) {
        static TeamMemberResponse from(TeamMember member) {
            GamePlayer player = member.getGamePlayer();
            return new TeamMemberResponse(player.getId(), player.getName(), player.getSkillLevel(),
                    member.getPosition());
        }
    }
}
