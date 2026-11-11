package com.gaeddong.footy.domain.balance.presentation.dto;

import com.gaeddong.footy.domain.balance.domain.BalanceReason;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateBalanceReviewRequest(
        @NotNull @Min(1) @Max(10) Integer score,
        List<@NotNull BalanceReason> reasons,
        @Size(max = 500) String comment
) {
}
