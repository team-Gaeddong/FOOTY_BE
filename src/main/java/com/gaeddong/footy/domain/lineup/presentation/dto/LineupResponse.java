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
        return of(created, true);
    }

    // skillVisible이 false면 실력(skillLevel, totalSkill)을 null로 내린다(D-20·D-24)
    public static LineupResponse of(CreatedLineup created, boolean skillVisible) {
        Lineup lineup = created.lineup();
        return new LineupResponse(
                lineup.getId(),
                lineup.getVersion(),
                lineup.getMethod(),
                lineup.isConfirmed(),
                created.teams().stream().map(team -> TeamResponse.of(team, skillVisible)).toList()
        );
    }

    public record TeamResponse(Long teamId, String name, Integer totalSkill, List<TeamMemberResponse> members) {
        static TeamResponse of(CreatedTeam created, boolean skillVisible) {
            Team team = created.team();
            return new TeamResponse(
                    team.getId(),
                    team.getName(),
                    skillVisible ? team.getTotalSkill() : null,
                    created.members().stream().map(member -> TeamMemberResponse.of(member, skillVisible)).toList()
            );
        }
    }

    public record TeamMemberResponse(Long gamePlayerId, String name, Integer skillLevel, Position position,
                                     boolean isSubstitute) {
        static TeamMemberResponse of(TeamMember member, boolean skillVisible) {
            GamePlayer player = member.getGamePlayer();
            return new TeamMemberResponse(player.getId(), player.getName(),
                    skillVisible ? player.getSkillLevel() : null,
                    member.getPosition(), player.isSubstitute());
        }
    }
}
