package com.gaeddong.footy.domain.member.domain;

import com.gaeddong.footy.domain.member.exception.MemberErrorCode;
import com.gaeddong.footy.global.error.FootyException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

/**
 * 우선순위가 있는 포지션 목록(D-13). 순서가 곧 우선순위이며 첫 번째가 priority 1이다.
 * 1~4개, 중복 불가. 회원 선호 포지션과 경기별 선수 포지션이 같은 규칙을 쓴다.
 */
public record PositionPriorities(List<Position> positions) {

    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = Position.values().length;

    public PositionPriorities {
        if (positions == null
                || positions.size() < MIN_SIZE
                || positions.size() > MAX_SIZE
                || positions.stream().anyMatch(Objects::isNull)
                || new HashSet<>(positions).size() != positions.size()) {
            throw new FootyException(MemberErrorCode.INVALID_POSITIONS);
        }
        positions = List.copyOf(positions);
    }

    /**
     * 각 포지션을 우선순위(1부터)와 함께 변환한다.
     */
    public <T> List<T> mapWithPriority(BiFunction<Position, Integer, T> mapper) {
        return IntStream.range(0, positions.size())
                .mapToObj(index -> mapper.apply(positions.get(index), index + 1))
                .toList();
    }
}
