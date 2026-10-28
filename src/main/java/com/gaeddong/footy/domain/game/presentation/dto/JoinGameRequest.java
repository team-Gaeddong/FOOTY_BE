package com.gaeddong.footy.domain.game.presentation.dto;

import com.gaeddong.footy.domain.member.domain.Position;

import java.util.List;

/**
 * 경기 참여 요청. 순서가 곧 우선순위다. 첫 번째가 priority 1(D-13).
 */
public record JoinGameRequest(List<Position> positions) {
}
