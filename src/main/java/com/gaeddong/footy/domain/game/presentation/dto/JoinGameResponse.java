package com.gaeddong.footy.domain.game.presentation.dto;

import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.member.domain.Position;

import java.time.LocalDateTime;
import java.util.List;

public record JoinGameResponse(
        Long id,
        Long gameId,
        Long memberId,
        String name,
        Integer skillLevel,
        boolean isSubstitute,
        List<Position> positions,
        LocalDateTime joinedAt
) {
    public static JoinGameResponse of(GamePlayer player, List<PlayerPosition> positions) {
        return new JoinGameResponse(
                player.getId(),
                player.getGame().getId(),
                player.getMember().getId(),
                player.getName(),
                player.getSkillLevel(),
                player.isSubstitute(),
                positions.stream().map(PlayerPosition::getPosition).toList(),
                player.getJoinedAt()
        );
    }
}
