package com.gaeddong.footy.domain.poll.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 제목·선택지는 앞뒤 공백을 지운 뒤 검사한다. 선택지 개수(2~10)와 중복은 서비스에서 POLL_400_1로 거부한다.
 * allowMultiple을 생략하면 하나만 고르는 투표, deadline을 생략하면 마감 없는 투표다.
 */
public record CreatePollRequest(
        @NotBlank @Size(max = 100) String title,
        @NotNull List<@NotBlank @Size(max = 100) String> options,
        Boolean allowMultiple,
        LocalDateTime deadline
) {
    public CreatePollRequest {
        if (title != null) {
            title = title.strip();
        }
        if (options != null) {
            options = options.stream().map(option -> option == null ? null : option.strip()).toList();
        }
        if (allowMultiple == null) {
            allowMultiple = false;
        }
    }
}
