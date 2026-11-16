package com.gaeddong.footy.domain.mvp.presentation.dto;

import com.gaeddong.footy.domain.mvp.service.MvpResultService.MyVote;

public record MyMvpVoteResponse(
        Long gameId,
        boolean voted,
        Long gamePlayerId
) {

    public static MyMvpVoteResponse of(Long gameId, MyVote myVote) {
        return new MyMvpVoteResponse(gameId, myVote.voted(), myVote.gamePlayerId());
    }
}
