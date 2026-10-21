package com.gaeddong.footy.domain.game.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateGameRequest(
        @NotBlank @Size(max = 100) String title,
        @NotNull LocalDateTime playDate,
        @NotBlank @Size(max = 200) String location,
        @NotNull @Positive Integer maxPlayers,
        Integer teamCount
) {
    public CreateGameRequest {
        if (teamCount == null) {
            teamCount = 2;
        }
    }
}
