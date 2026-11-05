package com.gaeddong.footy.domain.notice.facade;

import com.gaeddong.footy.domain.notice.presentation.dto.CreateNoticeRequest;
import com.gaeddong.footy.domain.notice.presentation.dto.CreateNoticeResponse;
import com.gaeddong.footy.domain.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoticeFacade {

    private final NoticeService noticeService;

    public CreateNoticeResponse create(Long requesterId, Long gameId, CreateNoticeRequest request) {
        return CreateNoticeResponse.from(
                noticeService.create(requesterId, gameId, request.title(), request.content())
        );
    }
}
