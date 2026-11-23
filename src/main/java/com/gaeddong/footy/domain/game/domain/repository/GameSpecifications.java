package com.gaeddong.footy.domain.game.domain.repository;

import com.gaeddong.footy.domain.game.domain.Game;
import com.gaeddong.footy.domain.game.domain.GamePlayer;
import com.gaeddong.footy.domain.game.domain.GameStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Collection;

/**
 * 경기 목록 조회 조건(D-37). 필요한 조건만 골라 AND로 묶어 쓴다.
 */
public final class GameSpecifications {

    private GameSpecifications() {
    }

    public static Specification<Game> notDeleted() {
        return (game, query, cb) -> cb.isFalse(game.get("deleted"));
    }

    public static Specification<Game> statusIn(Collection<GameStatus> statuses) {
        return (game, query, cb) -> game.get("status").in(statuses);
    }

    public static Specification<Game> hostedBy(Long memberId) {
        return (game, query, cb) -> cb.equal(game.get("host").get("id"), memberId);
    }

    // 파티장은 경기 생성 때 자동 참가하므로 직접 만든 경기도 포함된다
    public static Specification<Game> joinedBy(Long memberId) {
        return (game, query, cb) -> {
            Subquery<Long> joined = query.subquery(Long.class);
            Root<GamePlayer> player = joined.from(GamePlayer.class);
            joined.select(player.get("id"))
                    .where(cb.equal(player.get("game"), game), cb.equal(player.get("member").get("id"), memberId));
            return cb.exists(joined);
        };
    }

    // 그 날 0시부터
    public static Specification<Game> playedOnOrAfter(LocalDate date) {
        return (game, query, cb) -> cb.greaterThanOrEqualTo(game.get("playDate"), date.atStartOfDay());
    }

    // 그 날 마지막 순간까지(다음 날 0시 전)
    public static Specification<Game> playedOnOrBefore(LocalDate date) {
        return (game, query, cb) -> cb.lessThan(game.get("playDate"), date.plusDays(1).atStartOfDay());
    }
}
