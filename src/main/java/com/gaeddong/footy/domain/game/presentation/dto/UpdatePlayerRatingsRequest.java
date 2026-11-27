package com.gaeddong.footy.domain.game.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 범위(1.0~10.0)와 0.5 단위는 서비스에서 확인한다(GAME_400_5).
 */
public record UpdatePlayerRatingsRequest(
        @NotEmpty List<@NotNull @Valid PlayerRating> players
) {
    public record PlayerRating(
            @NotNull Long gamePlayerId,
            @NotNull BigDecimal rating
    ) {
    }
}
