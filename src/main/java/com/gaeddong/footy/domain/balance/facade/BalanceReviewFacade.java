package com.gaeddong.footy.domain.balance.facade;

import com.gaeddong.footy.domain.balance.presentation.dto.CreateBalanceReviewRequest;
import com.gaeddong.footy.domain.balance.presentation.dto.CreateBalanceReviewResponse;
import com.gaeddong.footy.domain.balance.service.BalanceReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BalanceReviewFacade {

    private final BalanceReviewService balanceReviewService;

    public CreateBalanceReviewResponse create(Long writerId, Long gameId, CreateBalanceReviewRequest request) {
        return CreateBalanceReviewResponse.from(
                balanceReviewService.create(writerId, gameId, request.score(), request.reasons(), request.comment())
        );
    }
}
