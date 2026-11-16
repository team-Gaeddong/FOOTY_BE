package com.gaeddong.footy.domain.mvp.presentation.dto;

import com.gaeddong.footy.domain.mvp.service.MvpResultService.Candidate;
import com.gaeddong.footy.domain.mvp.service.MvpResultService.Result;

import java.util.List;

public record MvpResultResponse(
        Long gameId,
        long requiredCount,
        long votedCount,
        List<Long> mvpGamePlayerIds,
        List<CandidateResponse> candidates
) {

    public static MvpResultResponse of(Long gameId, Result result) {
        return new MvpResultResponse(
                gameId,
                result.requiredCount(),
                result.votedCount(),
                result.mvpGamePlayerIds(),
                result.candidates().stream().map(CandidateResponse::from).toList()
        );
    }

    public record CandidateResponse(Long gamePlayerId, String name, long voteCount) {

        static CandidateResponse from(Candidate candidate) {
            return new CandidateResponse(candidate.gamePlayerId(), candidate.name(), candidate.voteCount());
        }
    }
}
