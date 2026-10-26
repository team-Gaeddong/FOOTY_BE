package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.domain.repository.PlayerPositionRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.PositionPriorities;
import com.gaeddong.footy.global.error.FootyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlayerPositionService {

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final PlayerPositionRepository playerPositionRepository;

    public PlayerPositionService(GameRepository gameRepository, GamePlayerRepository gamePlayerRepository,
                                 PlayerPositionRepository playerPositionRepository) {
        this.gameRepository = gameRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.playerPositionRepository = playerPositionRepository;
    }

    /**
     * 경기별 선수 포지션을 전부 교체한다. 파티장만, 모집 중인 경기에서만 할 수 있다(D-13).
     */
    @Transactional
    public void updatePositions(Long requesterId, Long gameId, Long gamePlayerId, List<Position> positions) {
        PositionPriorities priorities = new PositionPriorities(positions);
        Game game = gameRepository.findById(gameId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new FootyException(GameErrorCode.GAME_NOT_FOUND));
        if (!game.getHost().getId().equals(requesterId)) {
            throw new FootyException(GameErrorCode.NOT_GAME_HOST);
        }
        if (game.getStatus() != GameStatus.RECRUITING) {
            throw new FootyException(GameErrorCode.GAME_NOT_RECRUITING);
        }
        GamePlayer player = gamePlayerRepository.findByIdAndGameId(gamePlayerId, gameId)
                .orElseThrow(() -> new FootyException(GameErrorCode.GAME_PLAYER_NOT_FOUND));

        playerPositionRepository.deleteAllByGamePlayerId(player.getId());
        playerPositionRepository.saveAll(toPlayerPositions(player, priorities));
    }

    private List<PlayerPosition> toPlayerPositions(GamePlayer player, PositionPriorities priorities) {
        return priorities.mapWithPriority((position, priority) -> PlayerPosition.builder()
                .gamePlayer(player)
                .position(position)
                .priority(priority)
                .build());
    }
}
