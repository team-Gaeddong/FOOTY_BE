package com.gaeddong.footy.domain.mvp.presentation.dto;

import com.gaeddong.footy.domain.mvp.domain.MvpVote;

import java.time.LocalDateTime;

public record CreateMvpVoteResponse(
        Long id,
        Long gameId,
        Long gamePlayerId,
        LocalDateTime createdAt
) {

    public static CreateMvpVoteResponse from(MvpVote vote) {
        return new CreateMvpVoteResponse(
                vote.getId(),
                vote.getGame().getId(),
                vote.getGamePlayer().getId(),
                vote.getCreatedAt()
        );
    }
}
