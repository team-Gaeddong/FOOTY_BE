package com.gaeddong.footy.domain.balance.facade;

import com.gaeddong.footy.domain.balance.presentation.dto.BalanceSummaryResponse;
import com.gaeddong.footy.domain.balance.presentation.dto.CreateBalanceReviewRequest;
import com.gaeddong.footy.domain.balance.presentation.dto.CreateBalanceReviewResponse;
import com.gaeddong.footy.domain.balance.service.BalanceReviewService;
import com.gaeddong.footy.domain.balance.service.BalanceSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BalanceReviewFacade {

    private final BalanceReviewService balanceReviewService;
    private final BalanceSummaryService balanceSummaryService;

    public CreateBalanceReviewResponse create(Long writerId, Long gameId, CreateBalanceReviewRequest request) {
        return CreateBalanceReviewResponse.from(
                balanceReviewService.create(writerId, gameId, request.score(), request.reasons(), request.comment())
        );
    }

    public BalanceSummaryResponse summarize(Long requesterId, Long gameId) {
        return BalanceSummaryResponse.of(gameId, balanceSummaryService.summarize(requesterId, gameId));
    }
}
