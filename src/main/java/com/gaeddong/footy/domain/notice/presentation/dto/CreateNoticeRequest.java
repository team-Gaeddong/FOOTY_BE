package com.gaeddong.footy.domain.notice.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNoticeRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank String content
) {
}
