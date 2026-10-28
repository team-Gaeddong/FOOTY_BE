package com.gaeddong.footy.domain.game.presentation.dto;

import com.gaeddong.footy.domain.member.domain.Position;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 비회원 선수 등록 요청. 포지션은 순서가 곧 우선순위다(D-13). 실력은 생략할 수 있다.
 */
public record RegisterPlayerRequest(
        @NotBlank @Size(max = 30) String name,
        List<Position> positions,
        @Min(1) @Max(10) Integer skillLevel
) {
    public RegisterPlayerRequest {
        if (name != null) {
            name = name.strip();
        }
    }
}
