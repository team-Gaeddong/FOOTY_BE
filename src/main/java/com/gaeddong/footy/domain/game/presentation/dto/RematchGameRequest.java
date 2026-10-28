package com.gaeddong.footy.domain.game.presentation.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RematchGameRequest(@NotNull LocalDateTime playDate) {
}
