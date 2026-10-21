package com.gaeddong.footy.domain.auth.exception;

import com.gaeddong.footy.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_LOGIN_CODE(HttpStatus.UNAUTHORIZED, "AUTH_401_1", "로그인 코드가 유효하지 않거나 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_2", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_3", "만료된 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_4", "refresh 토큰이 유효하지 않습니다. 다시 로그인해 주세요."),
    LOGGED_OUT_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_5", "로그아웃한 토큰입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
