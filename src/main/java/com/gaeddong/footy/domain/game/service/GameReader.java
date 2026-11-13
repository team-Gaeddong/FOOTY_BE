package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 경기를 찾는 공통 규칙. 삭제된 경기(is_deleted)는 없는 경기로 보고 404 GAME_404_1을 낸다.
 * 트랜잭션은 호출하는 서비스의 것을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class GameReader {

    private final GameRepository gameRepository;

    public Game getGame(Long gameId) {
        return existing(gameRepository.findById(gameId));
    }

    /**
     * 파티장 정보를 응답에 담을 때 쓴다. 파티장(host)을 같은 쿼리로 가져온다.
     */
    public Game getGameWithHost(Long gameId) {
        return existing(gameRepository.findWithHostById(gameId));
    }

    /**
     * 파티장 전용 동작에 쓴다. 요청자가 파티장이 아니면 403 GAME_403_1이다.
     */
    public Game getGameAsHost(Long gameId, Long requesterId) {
        Game game = getGame(gameId);
        if (!game.isHostedBy(requesterId)) {
            throw new FootyException(GameErrorCode.NOT_GAME_HOST);
        }
        return game;
    }

    private Game existing(Optional<Game> game) {
        return game.filter(found -> !found.isDeleted())
                .orElseThrow(() -> new FootyException(GameErrorCode.GAME_NOT_FOUND));
    }
}
