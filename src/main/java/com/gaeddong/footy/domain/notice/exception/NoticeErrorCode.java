package com.gaeddong.footy.domain.notice.exception;

import com.gaeddong.footy.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NoticeErrorCode implements ErrorCode {

    NOT_GAME_PARTICIPANT(HttpStatus.FORBIDDEN, "NOTICE_403_1", "경기 참가자만 공지사항을 조회할 수 있습니다."),
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTICE_404_1", "공지사항을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
