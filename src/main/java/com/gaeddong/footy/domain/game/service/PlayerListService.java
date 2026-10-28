package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.PlayerPosition;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.domain.repository.PlayerPositionRepository;
import com.gaeddong.footy.domain.game.exception.GameErrorCode;
import com.gaeddong.footy.domain.member.domain.Position;
import com.gaeddong.footy.global.error.FootyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerListService {

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final PlayerPositionRepository playerPositionRepository;

    /**
     * 경기 참가 선수를 참여 순으로 조회한다. 로그인한 회원 누구나 볼 수 있고, 실력은 파티장에게만 보인다(D-20).
     */
    @Transactional(readOnly = true)
    public List<ListedPlayer> list(Long memberId, Long gameId) {
        Game game = gameRepository.findById(gameId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new FootyException(GameErrorCode.GAME_NOT_FOUND));
        boolean host = game.getHost().getId().equals(memberId);

        List<GamePlayer> players = gamePlayerRepository.findAllByGameIdOrderByJoinedAtAscIdAsc(gameId);
        if (players.isEmpty()) {
            return List.of();
        }
        // 선수마다 따로 조회하지 않도록 포지션을 한 번에 가져온다
        Map<Long, List<Position>> positionsByPlayerId = playerPositionRepository
                .findAllByGamePlayerIdInOrderByPriorityAsc(players.stream().map(GamePlayer::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(position -> position.getGamePlayer().getId(),
                        Collectors.mapping(PlayerPosition::getPosition, Collectors.toList())));

        return players.stream()
                .map(player -> new ListedPlayer(
                        player,
                        positionsByPlayerId.getOrDefault(player.getId(), List.of()),
                        host ? player.getSkillLevel() : null))
                .toList();
    }

    /**
     * skillLevel은 요청자에게 보여줄 값이다. 파티장이 아니면 NULL.
     */
    public record ListedPlayer(GamePlayer player, List<Position> positions, Integer skillLevel) {
    }
}
