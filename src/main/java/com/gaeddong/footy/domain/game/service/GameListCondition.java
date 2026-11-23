package com.gaeddong.footy.domain.game.service;

import com.gaeddong.footy.domain.game.domain.GameStatus;

import java.time.LocalDate;
import java.util.Collection;

/**
 * 경기 목록을 좁히는 선택 조건(D-37). 비어 있거나 null인 항목은 조건에서 뺀다.
 *
 * @param statuses 경기 상태
 * @param role     요청자와의 관계
 * @param from     경기 날짜의 시작(그 날 포함)
 * @param to       경기 날짜의 끝(그 날 포함)
 */
public record GameListCondition(Collection<GameStatus> statuses, GameListRole role, LocalDate from, LocalDate to) {

    public static GameListCondition none() {
        return new GameListCondition(null, null, null, null);
    }

    boolean hasStatuses() {
        return statuses != null && !statuses.isEmpty();
    }

    boolean hasReversedDates() {
        return from != null && to != null && from.isAfter(to);
    }
}
