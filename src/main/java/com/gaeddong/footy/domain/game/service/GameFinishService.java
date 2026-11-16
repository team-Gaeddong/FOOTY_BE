package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameFinishService {

    private final GameReader gameReader;

    /**
     * 파티장이 팀 확정(CONFIRMED)된 경기를 종료한다(D-32).
     * 종료한 경기에서만 밸런스 작성·MVP 투표·재경기 생성을 할 수 있다.
     */
    @Transactional
    public void finish(Long requesterId, Long gameId) {
        Game game = gameReader.getGameAsHost(gameId, requesterId);
        if (!game.isConfirmed()) {
            throw new FootyException(GameErrorCode.GAME_NOT_CONFIRMED);
        }
        game.finish();
    }
}
