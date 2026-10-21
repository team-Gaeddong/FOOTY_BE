package com.gaeddong.footy.global.error;

import org.springframework.http.HttpStatus;

/**
 * 에러 응답의 상태·코드·메시지(D-04). 코드는 {도메인}_{HTTP 상태}_{순번} 형식이다.
 * 도메인별 구현은 각 도메인의 exception 패키지에 둔다.
 */
public interface ErrorCode {

    HttpStatus getStatus();

    String getCode();

    String getMessage();
}
