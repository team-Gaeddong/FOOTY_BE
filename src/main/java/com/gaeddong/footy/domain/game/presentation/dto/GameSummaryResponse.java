package com.gaeddong.footy.domain.game.presentation.dto;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GameStatus;

import java.time.LocalDateTime;

public record GameSummaryResponse(
        Long id,
        Long hostId,
        String title,
        LocalDateTime playDate,
        String location,
        int maxPlayers,
        int teamCount,
        GameStatus status
) {
    public static GameSummaryResponse from(Game game) {
        return new GameSummaryResponse(
                game.getId(),
                game.getHost().getId(),
                game.getTitle(),
                game.getPlayDate(),
                game.getLocation(),
                game.getMaxPlayers(),
                game.getTeamCount(),
                game.getStatus()
        );
    }
}
