package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.member.service.MemberRatingService;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameFinishService {

    private final GameReader gameReader;
    private final MemberRatingService memberRatingService;

    /**
     * 파티장이 팀 확정(CONFIRMED)된 경기를 종료한다(D-34).
     * 종료한 경기에서만 밸런스 작성·MVP 투표·재경기 생성을 할 수 있다.
     * 종료와 함께 참가 회원의 평점을 다시 계산한다.
     */
    @Transactional
    public void finish(Long requesterId, Long gameId) {
        Game game = gameReader.getGameAsHost(gameId, requesterId);
        if (!game.isConfirmed()) {
            throw new FootyException(GameErrorCode.GAME_NOT_CONFIRMED);
        }
        game.finish();
        // 이 경기에서 받은 점수가 참가 회원의 평점에 바로 반영된다(D-38)
        memberRatingService.refreshForGame(gameId);
    }
}
