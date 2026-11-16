package com.gaeddong.footy.domain.mvp.facade;

import com.gaeddong.footy.domain.mvp.presentation.dto.CreateMvpVoteRequest;
import com.gaeddong.footy.domain.mvp.presentation.dto.CreateMvpVoteResponse;
import com.gaeddong.footy.domain.mvp.presentation.dto.MvpResultResponse;
import com.gaeddong.footy.domain.mvp.presentation.dto.MyMvpVoteResponse;
import com.gaeddong.footy.domain.mvp.service.MvpResultService;
import com.gaeddong.footy.domain.mvp.service.MvpVoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MvpVoteFacade {

    private final MvpVoteService mvpVoteService;
    private final MvpResultService mvpResultService;

    public CreateMvpVoteResponse vote(Long voterId, Long gameId, CreateMvpVoteRequest request) {
        return CreateMvpVoteResponse.from(mvpVoteService.vote(voterId, gameId, request.gamePlayerId()));
    }

    public MvpResultResponse getResult(Long requesterId, Long gameId) {
        return MvpResultResponse.of(gameId, mvpResultService.getResult(requesterId, gameId));
    }

    public MyMvpVoteResponse getMyVote(Long requesterId, Long gameId) {
        return MyMvpVoteResponse.of(gameId, mvpResultService.getMyVote(requesterId, gameId));
    }
}
