package com.gaeddong.footy.domain.notice.presentation.dto;

import com.gaeddong.footy.domain.notice.domain.Notice;

import java.util.List;

public record ListNoticesResponse(List<NoticeResponse> notices) {

    public static ListNoticesResponse from(List<Notice> notices) {
        return new ListNoticesResponse(notices.stream().map(NoticeResponse::from).toList());
    }
}
