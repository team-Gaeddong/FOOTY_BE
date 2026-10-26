package com.gaeddong.footy.domain.member.presentation.dto.request;

import com.gaeddong.footy.domain.member.domain.Position;

import java.util.List;

/**
 * 순서가 곧 우선순위다. 첫 번째가 priority 1(D-13).
 */
public record UpdatePositionsRequest(List<Position> positions) {
}
