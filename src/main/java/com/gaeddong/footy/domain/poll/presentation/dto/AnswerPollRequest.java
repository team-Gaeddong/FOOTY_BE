package com.gaeddong.footy.domain.poll.presentation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 내 응답 전체. 보낸 선택지로 바꾸고, 빈 목록이면 투표를 취소한다.
 */
public record AnswerPollRequest(@NotNull List<@NotNull Long> optionIds) {
}
