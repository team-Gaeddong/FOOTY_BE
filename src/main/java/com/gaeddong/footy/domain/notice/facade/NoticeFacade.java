package com.gaeddong.footy.domain.notice.facade;

import com.gaeddong.footy.domain.notice.presentation.dto.CreateNoticeRequest;
import com.gaeddong.footy.domain.notice.presentation.dto.CreateNoticeResponse;
import com.gaeddong.footy.domain.notice.presentation.dto.ListNoticesResponse;
import com.gaeddong.footy.domain.notice.presentation.dto.NoticeResponse;
import com.gaeddong.footy.domain.notice.service.NoticeQueryService;
import com.gaeddong.footy.domain.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoticeFacade {

    private final NoticeService noticeService;
    private final NoticeQueryService noticeQueryService;

    public CreateNoticeResponse create(Long requesterId, Long gameId, CreateNoticeRequest request) {
        return CreateNoticeResponse.from(
                noticeService.create(requesterId, gameId, request.title(), request.content())
        );
    }

    public ListNoticesResponse list(Long requesterId, Long gameId) {
        return ListNoticesResponse.from(noticeQueryService.list(requesterId, gameId));
    }

    public NoticeResponse get(Long requesterId, Long gameId, Long noticeId) {
        return NoticeResponse.from(noticeQueryService.get(requesterId, gameId, noticeId));
    }
}
