package com.gaeddong.footy.domain.mvp.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record CreateMvpVoteRequest(
        @NotNull Long gamePlayerId
) {
}
