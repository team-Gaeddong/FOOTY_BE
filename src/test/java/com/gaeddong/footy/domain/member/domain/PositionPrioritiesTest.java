package com.gaeddong.footy.domain.member.domain;

import com.gaeddong.footy.domain.member.exception.MemberErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PositionPrioritiesTest {

    @Test
    void 순서대로_우선순위가_1부터_붙는다() {
        PositionPriorities priorities = new PositionPriorities(List.of(Position.FW, Position.MF));

        assertThat(priorities.mapWithPriority((position, priority) -> position + ":" + priority))
                .containsExactly("FW:1", "MF:2");
    }

    @Test
    void 네_포지션을_모두_고를_수_있다() {
        PositionPriorities priorities = new PositionPriorities(List.of(Position.values()));

        assertThat(priorities.positions()).containsExactly(Position.GK, Position.DF, Position.MF, Position.FW);
    }

    @Test
    void 원본_목록이_바뀌어도_영향받지_않는다() {
        List<Position> source = new ArrayList<>(List.of(Position.GK));
        PositionPriorities priorities = new PositionPriorities(source);

        source.add(Position.FW);

        assertThat(priorities.positions()).containsExactly(Position.GK);
    }

    @Test
    void 비어_있거나_null이면_거부한다() {
        assertInvalid(null);
        assertInvalid(List.of());
        assertInvalid(Arrays.asList(Position.FW, null));
    }

    @Test
    void 중복된_포지션은_거부한다() {
        assertInvalid(List.of(Position.FW, Position.MF, Position.FW));
    }

    @Test
    void 다섯_개_이상은_거부한다() {
        assertInvalid(List.of(Position.GK, Position.DF, Position.MF, Position.FW, Position.GK));
    }

    private static void assertInvalid(List<Position> positions) {
        assertThatThrownBy(() -> new PositionPriorities(positions))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(MemberErrorCode.INVALID_POSITIONS);
    }
}
