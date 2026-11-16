package com.gaeddong.footy.domain.mvp.presentation;

import com.gaeddong.footy.domain.mvp.facade.MvpVoteFacade;
import com.gaeddong.footy.domain.mvp.presentation.dto.CreateMvpVoteRequest;
import com.gaeddong.footy.domain.mvp.presentation.dto.CreateMvpVoteResponse;
import com.gaeddong.footy.domain.mvp.presentation.dto.MvpResultResponse;
import com.gaeddong.footy.domain.mvp.presentation.dto.MyMvpVoteResponse;
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
@RequestMapping("/games/{gameId}/mvp-votes")
@RequiredArgsConstructor
public class MvpVoteController {

    private final MvpVoteFacade mvpVoteFacade;

    @PostMapping
    public ResponseEntity<CreateMvpVoteResponse> vote(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long gameId,
            @Valid @RequestBody CreateMvpVoteRequest request
    ) {
        CreateMvpVoteResponse response = mvpVoteFacade.vote(memberId, gameId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/result")
    public MvpResultResponse getResult(@AuthenticationPrincipal Long memberId, @PathVariable Long gameId) {
        return mvpVoteFacade.getResult(memberId, gameId);
    }

    @GetMapping("/me")
    public MyMvpVoteResponse getMyVote(@AuthenticationPrincipal Long memberId, @PathVariable Long gameId) {
        return mvpVoteFacade.getMyVote(memberId, gameId);
    }
}
