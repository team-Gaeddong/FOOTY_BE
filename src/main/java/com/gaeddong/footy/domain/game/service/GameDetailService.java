package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameDetailService {

    private final GameReader gameReader;
    private final GamePlayerRepository gamePlayerRepository;

    /**
     * 경기 상세를 조회한다. 로그인한 회원 누구나 볼 수 있다(#54).
     */
    @Transactional(readOnly = true)
    public GameDetail get(Long requesterId, Long gameId) {
        Game game = gameReader.getGameWithHost(gameId);
        return new GameDetail(
                game,
                gamePlayerRepository.countByGameId(gameId),
                game.isHostedBy(requesterId),
                gamePlayerRepository.existsByGameIdAndMemberId(gameId, requesterId));
    }

    /**
     * playerCount는 후보를 포함한 참가 선수 수, host·joined는 요청자 기준이다.
     */
    public record GameDetail(Game game, long playerCount, boolean host, boolean joined) {
    }
}
