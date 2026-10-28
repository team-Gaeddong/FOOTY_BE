package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RematchGameService {

    private final GameRepository gameRepository;
    private final GameService gameService;
    private final Clock clock;

    @Transactional
    public Game create(Long requesterId, Long sourceGameId, LocalDateTime playDate) {
        Game source = gameRepository.findById(sourceGameId)
                .filter(game -> !game.isDeleted())
                .orElseThrow(() -> new FootyException(GameErrorCode.GAME_NOT_FOUND));

        if (!source.getHost().getId().equals(requesterId)) {
            throw new FootyException(GameErrorCode.NOT_GAME_HOST);
        }
        if (source.getStatus() != GameStatus.FINISHED) {
            throw new FootyException(GameErrorCode.GAME_NOT_FINISHED);
        }
        if (!playDate.isAfter(LocalDateTime.now(clock))) {
            throw new FootyException(GameErrorCode.PAST_PLAY_DATE);
        }

        return gameService.create(
                requesterId,
                source.getTitle(),
                playDate,
                source.getLocation(),
                source.getMaxPlayers(),
                source.getTeamCount()
        );
    }
}
