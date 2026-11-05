package com.gaeddong.footy.domain.notice.presentation;

import com.gaeddong.footy.domain.notice.facade.NoticeFacade;
import com.gaeddong.footy.domain.notice.presentation.dto.CreateNoticeRequest;
import com.gaeddong.footy.domain.notice.presentation.dto.CreateNoticeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/games/{gameId}/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeFacade noticeFacade;

    @PostMapping
    public ResponseEntity<CreateNoticeResponse> create(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long gameId,
            @Valid @RequestBody CreateNoticeRequest request
    ) {
        CreateNoticeResponse response = noticeFacade.create(memberId, gameId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
