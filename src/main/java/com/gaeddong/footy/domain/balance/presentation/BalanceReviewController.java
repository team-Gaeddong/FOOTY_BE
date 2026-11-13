package com.gaeddong.footy.domain.balance.presentation;

import com.gaeddong.footy.domain.balance.facade.BalanceReviewFacade;
import com.gaeddong.footy.domain.balance.presentation.dto.BalanceSummaryResponse;
import com.gaeddong.footy.domain.balance.presentation.dto.CreateBalanceReviewRequest;
import com.gaeddong.footy.domain.balance.presentation.dto.CreateBalanceReviewResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/games/{gameId}/balance-reviews")
@RequiredArgsConstructor
public class BalanceReviewController {

    private final BalanceReviewFacade balanceReviewFacade;

    @PostMapping
    public ResponseEntity<CreateBalanceReviewResponse> create(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long gameId,
            @Valid @RequestBody CreateBalanceReviewRequest request
    ) {
        CreateBalanceReviewResponse response = balanceReviewFacade.create(memberId, gameId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/summary")
    public BalanceSummaryResponse summarize(@AuthenticationPrincipal Long memberId, @PathVariable Long gameId) {
        return balanceReviewFacade.summarize(memberId, gameId);
    }
}
