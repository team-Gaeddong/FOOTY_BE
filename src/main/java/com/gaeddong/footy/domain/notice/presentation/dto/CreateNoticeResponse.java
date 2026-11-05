package com.gaeddong.footy.domain.notice.presentation.dto;

import com.gaeddong.footy.domain.notice.domain.Notice;

import java.time.LocalDateTime;

public record CreateNoticeResponse(
        Long id,
        String title,
        String content,
        LocalDateTime createdAt
) {

    public static CreateNoticeResponse from(Notice notice) {
        return new CreateNoticeResponse(notice.getId(), notice.getTitle(), notice.getContent(), notice.getCreatedAt());
    }
}
