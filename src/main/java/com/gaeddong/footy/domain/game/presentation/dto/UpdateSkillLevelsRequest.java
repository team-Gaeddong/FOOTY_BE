package com.gaeddong.footy.domain.game.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateSkillLevelsRequest(
        @NotEmpty List<@NotNull @Valid PlayerSkillLevel> players
) {
    public record PlayerSkillLevel(
            @NotNull Long gamePlayerId,
            @NotNull @Min(1) @Max(10) Integer skillLevel
    ) {
    }
}
