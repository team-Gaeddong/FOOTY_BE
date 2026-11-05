package com.gaeddong.footy.domain.balance.presentation.dto;

import com.gaeddong.footy.domain.balance.domain.BalanceReason;
import com.gaeddong.footy.domain.balance.domain.BalanceReview;
import com.gaeddong.footy.domain.balance.domain.BalanceReviewReason;
import com.gaeddong.footy.domain.balance.service.BalanceReviewService.CreatedReview;

import java.time.LocalDateTime;
import java.util.List;

public record CreateBalanceReviewResponse(
        Long id,
        Long gameId,
        int score,
        List<BalanceReason> reasons,
        String comment,
        LocalDateTime createdAt
) {

    public static CreateBalanceReviewResponse from(CreatedReview created) {
        BalanceReview review = created.review();
        return new CreateBalanceReviewResponse(
                review.getId(),
                review.getGame().getId(),
                review.getScore(),
                created.reasons().stream().map(BalanceReviewReason::getReason).toList(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
