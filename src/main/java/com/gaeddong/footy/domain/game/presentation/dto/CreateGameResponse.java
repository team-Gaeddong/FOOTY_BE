package com.gaeddong.footy.domain.game.presentation.dto;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GameStatus;

import java.time.LocalDateTime;

public record CreateGameResponse(
        Long id,
        Long hostId,
        String title,
        LocalDateTime playDate,
        String location,
        int maxPlayers,
        int teamCount,
        GameStatus status,
        LocalDateTime createdAt
) {
    public static CreateGameResponse from(Game game) {
        return new CreateGameResponse(
                game.getId(),
                game.getHost().getId(),
                game.getTitle(),
                game.getPlayDate(),
                game.getLocation(),
                game.getMaxPlayers(),
                game.getTeamCount(),
                game.getStatus(),
                game.getCreatedAt()
        );
    }
}
