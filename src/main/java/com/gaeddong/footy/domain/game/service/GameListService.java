package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository.PlayerCount;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.global.error.FootyException;
import com.gaeddong.footy.global.error.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameListService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort LIST_ORDER = Sort.by(Sort.Direction.ASC, "playDate", "id");

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;

    /**
     * 삭제되지 않은 경기를 날짜순으로 조회한다(D-37).
     * statuses가 비어 있으면 모든 상태, role이 null이면 요청자와 관계없이 전체다.
     */
    @Transactional(readOnly = true)
    public Page<GameSummary> list(
            Long requesterId, Collection<GameStatus> statuses, GameListRole role, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new FootyException(GlobalErrorCode.INVALID_INPUT);
        }
        Page<Game> games = findGames(requesterId, allIfEmpty(statuses), role, PageRequest.of(page, size, LIST_ORDER));
        Map<Long, Long> playerCounts = countPlayers(games.getContent());
        return games.map(game -> new GameSummary(game, playerCounts.getOrDefault(game.getId(), 0L)));
    }

    private Set<GameStatus> allIfEmpty(Collection<GameStatus> statuses) {
        return statuses == null || statuses.isEmpty()
                ? EnumSet.allOf(GameStatus.class)
                : EnumSet.copyOf(statuses);
    }

    private Page<Game> findGames(Long requesterId, Set<GameStatus> statuses, GameListRole role, Pageable pageable) {
        if (role == GameListRole.HOST) {
            return gameRepository.findByDeletedFalseAndStatusInAndHostId(statuses, requesterId, pageable);
        }
        if (role == GameListRole.JOINED) {
            return gameRepository.findJoinedBy(requesterId, statuses, pageable);
        }
        return gameRepository.findByDeletedFalseAndStatusIn(statuses, pageable);
    }

    // 경기마다 세지 않고 한 쪽에 나온 경기를 한 번에 센다
    private Map<Long, Long> countPlayers(List<Game> games) {
        if (games.isEmpty()) {
            return Map.of();
        }
        List<Long> gameIds = games.stream().map(Game::getId).toList();
        return gamePlayerRepository.countPlayersByGameIds(gameIds).stream()
                .collect(Collectors.toMap(PlayerCount::getGameId, PlayerCount::getPlayerCount));
    }

    /**
     * playerCount는 후보를 포함한 참가 선수 수다(경기 상세 D-25와 같은 기준).
     */
    public record GameSummary(Game game, long playerCount) {
    }
}
