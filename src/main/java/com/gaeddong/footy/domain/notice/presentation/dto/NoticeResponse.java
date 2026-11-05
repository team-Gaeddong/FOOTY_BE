package com.gaeddong.footy.domain.notice.presentation.dto;

import com.gaeddong.footy.domain.notice.domain.Notice;

import java.time.LocalDateTime;

public record NoticeResponse(
        Long id,
        String title,
        String content,
        LocalDateTime createdAt
) {

    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(notice.getId(), notice.getTitle(), notice.getContent(), notice.getCreatedAt());
    }
}
