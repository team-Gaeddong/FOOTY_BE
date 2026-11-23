package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository;
import com.gaeddong.footy.domain.game.domain.repository.GamePlayerRepository.PlayerCount;
import com.gaeddong.footy.domain.game.domain.repository.GameRepository;
import com.gaeddong.footy.domain.game.domain.repository.GameSpecifications;
import com.gaeddong.footy.global.error.FootyException;
import com.gaeddong.footy.global.error.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameListService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort LIST_ORDER = Sort.by(Sort.Direction.ASC, "playDate", "id");

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;

    /**
     * 삭제되지 않은 경기를 날짜순으로 조회한다(D-37). 조건이 없으면 전체다.
     */
    @Transactional(readOnly = true)
    public Page<GameSummary> list(Long requesterId, GameListCondition condition, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE || condition.hasReversedDates()) {
            throw new FootyException(GlobalErrorCode.INVALID_INPUT);
        }
        Page<Game> games = gameRepository.findAll(
                toSpecification(requesterId, condition), PageRequest.of(page, size, LIST_ORDER));
        Map<Long, Long> playerCounts = countPlayers(games.getContent());
        return games.map(game -> new GameSummary(game, playerCounts.getOrDefault(game.getId(), 0L)));
    }

    private Specification<Game> toSpecification(Long requesterId, GameListCondition condition) {
        List<Specification<Game>> filters = new ArrayList<>();
        filters.add(GameSpecifications.notDeleted());
        if (condition.hasStatuses()) {
            filters.add(GameSpecifications.statusIn(condition.statuses()));
        }
        if (condition.role() == GameListRole.HOST) {
            filters.add(GameSpecifications.hostedBy(requesterId));
        }
        if (condition.role() == GameListRole.JOINED) {
            filters.add(GameSpecifications.joinedBy(requesterId));
        }
        if (condition.from() != null) {
            filters.add(GameSpecifications.playedOnOrAfter(condition.from()));
        }
        if (condition.to() != null) {
            filters.add(GameSpecifications.playedOnOrBefore(condition.to()));
        }
        return Specification.allOf(filters);
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
