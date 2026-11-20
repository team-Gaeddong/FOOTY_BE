package com.gaeddong.footy.domain.poll.presentation.dto;

import com.gaeddong.footy.domain.poll.domain.Poll;
import com.gaeddong.footy.domain.poll.service.PollQueryService.PollSummary;

import java.time.LocalDateTime;
import java.util.List;

public record ListPollsResponse(List<PollSummaryResponse> polls) {

    public static ListPollsResponse from(List<PollSummary> polls) {
        return new ListPollsResponse(polls.stream().map(PollSummaryResponse::from).toList());
    }

    /**
     * hasVoted는 요청자가 이 투표에 응답했는지다.
     */
    public record PollSummaryResponse(
            Long id,
            String title,
            boolean allowMultiple,
            LocalDateTime deadline,
            boolean isClosed,
            LocalDateTime createdAt,
            long voterCount,
            boolean hasVoted
    ) {
        static PollSummaryResponse from(PollSummary summary) {
            Poll poll = summary.poll();
            return new PollSummaryResponse(
                    poll.getId(),
                    poll.getTitle(),
                    poll.isAllowMultiple(),
                    poll.getDeadline(),
                    summary.closed(),
                    poll.getCreatedAt(),
                    summary.voterCount(),
                    summary.voted()
            );
        }
    }
}
