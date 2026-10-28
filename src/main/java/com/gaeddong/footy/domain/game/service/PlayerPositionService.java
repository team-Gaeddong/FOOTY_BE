package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.domain.repository.PlayerPositionRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.member.domain.MemberPosition;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.domain.member.domain.PositionPriorities;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerPositionService {

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final PlayerPositionRepository playerPositionRepository;

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

    /**
     * 매치 참여 때 회원 선호 포지션을 경기별 포지션으로 복사한다(D-13). 선호 포지션이 없으면 아무것도 하지 않는다.
     * 호출하는 쪽(매치 참여)의 트랜잭션에 참여한다.
     */
    @Transactional
    public void copyMemberPositions(GamePlayer player, List<MemberPosition> memberPositions) {
        if (memberPositions.isEmpty()) {
            return;
        }
        List<Position> positions = memberPositions.stream()
                .sorted(Comparator.comparingInt(MemberPosition::getPriority))
                .map(MemberPosition::getPosition)
                .toList();
        playerPositionRepository.saveAll(toPlayerPositions(player, new PositionPriorities(positions)));
    }

    private List<PlayerPosition> toPlayerPositions(GamePlayer player, PositionPriorities priorities) {
        return priorities.mapWithPriority((position, priority) -> PlayerPosition.builder()
                .gamePlayer(player)
                .position(position)
                .priority(priority)
                .build());
    }
}
