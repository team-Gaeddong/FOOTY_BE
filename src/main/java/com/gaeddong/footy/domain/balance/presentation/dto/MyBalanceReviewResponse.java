package com.gaeddong.footy.domain.balance.presentation.dto;

import com.gaeddong.footy.domain.balance.service.BalanceReviewService.CreatedReview;

import java.util.Optional;

/**
 * 작성 전이면 reviewed는 false, review는 null이다(D-39).
 */
public record MyBalanceReviewResponse(
        Long gameId,
        boolean reviewed,
        CreateBalanceReviewResponse review
) {

    public static MyBalanceReviewResponse of(Long gameId, Optional<CreatedReview> mine) {
        return new MyBalanceReviewResponse(
                gameId,
                mine.isPresent(),
                mine.map(CreateBalanceReviewResponse::from).orElse(null)
        );
    }
}
