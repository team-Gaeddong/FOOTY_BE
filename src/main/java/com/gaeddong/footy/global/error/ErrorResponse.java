package com.gaeddong.footy.global.error;

/**
 * 에러 응답 본문(D-04). HTTP 상태는 상태 코드로만 표현한다.
 */
public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }
}
