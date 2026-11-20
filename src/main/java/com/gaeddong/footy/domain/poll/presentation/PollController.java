package com.gaeddong.footy.domain.poll.presentation;

import com.gaeddong.footy.domain.poll.facade.PollFacade;
import com.gaeddong.footy.domain.poll.presentation.dto.AnswerPollRequest;
import com.gaeddong.footy.domain.poll.presentation.dto.CreatePollRequest;
import com.gaeddong.footy.domain.poll.presentation.dto.ListPollsResponse;
import com.gaeddong.footy.domain.poll.presentation.dto.PollResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/games/{gameId}/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollFacade pollFacade;

    @PostMapping
    public ResponseEntity<PollResponse> create(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long gameId,
            @Valid @RequestBody CreatePollRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pollFacade.create(memberId, gameId, request));
    }

    @GetMapping
    public ListPollsResponse list(@AuthenticationPrincipal Long memberId, @PathVariable Long gameId) {
        return pollFacade.list(memberId, gameId);
    }

    @GetMapping("/{pollId}")
    public PollResponse get(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long gameId,
            @PathVariable Long pollId
    ) {
        return pollFacade.get(memberId, gameId, pollId);
    }

    @PutMapping("/{pollId}/answers")
    public PollResponse answer(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long gameId,
            @PathVariable Long pollId,
            @Valid @RequestBody AnswerPollRequest request
    ) {
        return pollFacade.answer(memberId, gameId, pollId, request);
    }
}
