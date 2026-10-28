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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerRegisterService {

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final PlayerPositionRepository playerPositionRepository;
    private final Clock clock;

    /**
     * 파티장이 비회원 선수를 이름·포지션·실력과 함께 등록한다. 모집 중인 경기에서만 할 수 있다(#32).
     */
    @Transactional
    public RegisteredPlayer register(Long requesterId, Long gameId, String name, List<Position> positions,
                                     Integer skillLevel) {
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

        GamePlayer player = gamePlayerRepository.save(
                GamePlayer.registerGuest(game, name, skillLevel, LocalDateTime.now(clock)));
        List<PlayerPosition> savedPositions = playerPositionRepository.saveAll(
                priorities.mapWithPriority((position, priority) -> PlayerPosition.builder()
                        .gamePlayer(player)
                        .position(position)
                        .priority(priority)
                        .build()));
        return new RegisteredPlayer(player, savedPositions);
    }

    public record RegisteredPlayer(GamePlayer player, List<PlayerPosition> positions) {
    }
}
