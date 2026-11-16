package com.gaeddong.footy.domain.report.exception;

import com.gaeddong.footy.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements ErrorCode {

    SELF_REPORT(HttpStatus.BAD_REQUEST, "REPORT_400_1", "자기 자신은 신고할 수 없습니다."),
    NOT_GAME_PLAYER(HttpStatus.FORBIDDEN, "REPORT_403_1", "경기에 참가한 회원만 신고할 수 있습니다."),
    ALREADY_REPORTED(HttpStatus.CONFLICT, "REPORT_409_1", "이미 접수되어 처리 중인 신고입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
