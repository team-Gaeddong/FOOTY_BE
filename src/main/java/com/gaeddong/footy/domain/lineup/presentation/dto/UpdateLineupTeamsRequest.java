package com.gaeddong.footy.domain.lineup.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 최신 라인업의 팀 소속을 통째로 바꾼다. teams에는 라인업의 모든 팀이 한 번씩 나와야 하고,
 * 각 팀의 gamePlayerIds를 합치면 지금 라인업에 배정된 선수와 같아야 한다(서비스에서 확인한다).
 */
public record UpdateLineupTeamsRequest(
        @NotNull List<@NotNull @Valid TeamRoster> teams
) {
    public record TeamRoster(
            @NotNull Long teamId,
            @NotNull List<@NotNull Long> gamePlayerIds
    ) {
    }
}
