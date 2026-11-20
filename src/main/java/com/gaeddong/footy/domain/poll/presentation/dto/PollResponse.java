package com.gaeddong.footy.domain.poll.presentation.dto;

import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.poll.domain.Poll;
import com.gaeddong.footy.domain.poll.service.PollQueryService.OptionResult;
import com.gaeddong.footy.domain.poll.service.PollQueryService.PollDetail;

import java.time.LocalDateTime;
import java.util.List;

public record PollResponse(
        Long id,
        Long gameId,
        String title,
        boolean allowMultiple,
        LocalDateTime deadline,
        boolean isClosed,
        LocalDateTime createdAt,
        long voterCount,
        List<OptionResponse> options
) {
    public static PollResponse from(PollDetail detail) {
        Poll poll = detail.poll();
        return new PollResponse(
                poll.getId(),
                poll.getGame().getId(),
                poll.getTitle(),
                poll.isAllowMultiple(),
                poll.getDeadline(),
                detail.closed(),
                poll.getCreatedAt(),
                detail.voterCount(),
                detail.options().stream().map(OptionResponse::from).toList()
        );
    }

    /**
     * voters는 응답한 순서다. isSelected는 요청자가 고른 선택지인지다.
     */
    public record OptionResponse(
            Long id,
            String content,
            int voteCount,
            List<VoterResponse> voters,
            boolean isSelected
    ) {
        static OptionResponse from(OptionResult result) {
            return new OptionResponse(
                    result.option().getId(),
                    result.option().getContent(),
                    result.voters().size(),
                    result.voters().stream().map(VoterResponse::from).toList(),
                    result.selected()
            );
        }
    }

    public record VoterResponse(Long memberId, String nickname) {
        static VoterResponse from(Member member) {
            return new VoterResponse(member.getId(), member.getNickname());
        }
    }
}
