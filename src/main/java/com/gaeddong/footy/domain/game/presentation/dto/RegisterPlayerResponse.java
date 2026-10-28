package com.gaeddong.footy.domain.game.presentation.dto;

import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.member.domain.Position;

import java.time.LocalDateTime;
import java.util.List;

public record RegisterPlayerResponse(
        Long id,
        Long gameId,
        String name,
        Integer skillLevel,
        boolean isSubstitute,
        List<Position> positions,
        LocalDateTime joinedAt
) {
    public static RegisterPlayerResponse of(GamePlayer player, List<PlayerPosition> positions) {
        return new RegisterPlayerResponse(
                player.getId(),
                player.getGame().getId(),
                player.getName(),
                player.getSkillLevel(),
                player.isSubstitute(),
                positions.stream().map(PlayerPosition::getPosition).toList(),
                player.getJoinedAt()
        );
    }
}
