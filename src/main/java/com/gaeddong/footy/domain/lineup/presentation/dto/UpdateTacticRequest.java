package com.gaeddong.footy.domain.lineup.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 좌표는 경기장 크기 대비 퍼센트(0~100, 소수 2자리)다. team_member.pos_x·pos_y가 DECIMAL(5,2)라서다.
 */
public record UpdateTacticRequest(
        @Size(max = 20) String formation,
        @NotNull @Size(max = 11) List<@NotNull @Valid Placement> placements
) {
    public record Placement(
            @NotNull Long gamePlayerId,
            @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal posX,
            @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal posY
    ) {
    }
}
