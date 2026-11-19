package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameDeleteService {

    private final GameReader gameReader;

    /**
     * 파티장이 종료 전(모집 중·팀 확정) 경기를 소프트 삭제한다(D-33). 참가 선수 등 연관 데이터는 그대로 둔다.
     */
    @Transactional
    public void delete(Long requesterId, Long gameId) {
        Game game = gameReader.getGameAsHost(gameId, requesterId);
        if (game.isFinished()) {
            throw new FootyException(GameErrorCode.FINISHED_GAME_NOT_DELETABLE);
        }
        game.delete();
    }
}
