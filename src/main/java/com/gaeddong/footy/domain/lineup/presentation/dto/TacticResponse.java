package com.gaeddong.footy.domain.lineup.presentation.dto;

import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.lineup.domain.Team;
import com.gaeddong.footy.domain.lineup.domain.TeamMember;
import com.gaeddong.footy.domain.lineup.service.TacticService.Tactic;
import com.gaeddong.footy.domain.member.domain.Position;

import java.math.BigDecimal;
import java.util.List;

public record TacticResponse(Long teamId, String name, String formation, List<PlacedMemberResponse> members) {

    public static TacticResponse from(Tactic tactic) {
        Team team = tactic.team();
        return new TacticResponse(
                team.getId(),
                team.getName(),
                team.getFormation(),
                tactic.members().stream().map(PlacedMemberResponse::from).toList()
        );
    }

    // 배치 전이면 posX·posY는 null
    public record PlacedMemberResponse(Long gamePlayerId, String name, Position position, boolean isSubstitute,
                                       BigDecimal posX, BigDecimal posY) {
        static PlacedMemberResponse from(TeamMember member) {
            GamePlayer player = member.getGamePlayer();
            return new PlacedMemberResponse(player.getId(), player.getName(), member.getPosition(),
                    player.isSubstitute(), member.getPosX(), member.getPosY());
        }
    }
}
