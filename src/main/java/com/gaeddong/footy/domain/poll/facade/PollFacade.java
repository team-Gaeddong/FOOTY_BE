package com.gaeddong.footy.domain.poll.facade;

import com.gaeddong.footy.domain.poll.domain.Poll;
import com.gaeddong.footy.domain.poll.presentation.dto.AnswerPollRequest;
import com.gaeddong.footy.domain.poll.presentation.dto.CreatePollRequest;
import com.gaeddong.footy.domain.poll.presentation.dto.ListPollsResponse;
import com.gaeddong.footy.domain.poll.presentation.dto.PollResponse;
import com.gaeddong.footy.domain.poll.service.PollAnswerService;
import com.gaeddong.footy.domain.poll.service.PollQueryService;
import com.gaeddong.footy.domain.poll.service.PollService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PollFacade {

    private final PollService pollService;
    private final PollQueryService pollQueryService;
    private final PollAnswerService pollAnswerService;

    public PollResponse create(Long requesterId, Long gameId, CreatePollRequest request) {
        Poll poll = pollService.create(
                requesterId,
                gameId,
                request.title(),
                request.options(),
                request.allowMultiple(),
                request.deadline()
        );
        return PollResponse.from(pollQueryService.get(requesterId, gameId, poll.getId()));
    }

    public ListPollsResponse list(Long requesterId, Long gameId) {
        return ListPollsResponse.from(pollQueryService.list(requesterId, gameId));
    }

    public PollResponse get(Long requesterId, Long gameId, Long pollId) {
        return PollResponse.from(pollQueryService.get(requesterId, gameId, pollId));
    }

    // 바뀐 결과를 바로 보여주도록 응답 뒤 상세를 다시 읽는다
    public PollResponse answer(Long requesterId, Long gameId, Long pollId, AnswerPollRequest request) {
        pollAnswerService.answer(requesterId, gameId, pollId, request.optionIds());
        return PollResponse.from(pollQueryService.get(requesterId, gameId, pollId));
    }
}
