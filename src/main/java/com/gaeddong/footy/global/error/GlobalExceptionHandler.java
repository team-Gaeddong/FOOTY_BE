package com.gaeddong.footy.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FootyException.class)
    public ResponseEntity<ErrorResponse> handleFootyException(FootyException e) {
        return toResponse(e.getErrorCode());
    }

    /**
     * 본문을 읽지 못했거나(JSON 형식·타입 오류), 경로·쿼리 값의 타입이 맞지 않는 경우(예: /games/abc)다.
     * 둘 다 Spring의 ErrorResponse가 아니라서 따로 받지 않으면 500으로 떨어진다.
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleUnreadableInput(Exception e) {
        return toResponse(GlobalErrorCode.INVALID_INPUT);
    }

    /**
     * Spring MVC 예외(404·405, 검증 실패 등)는 ErrorResponse로 상태를 알려 주므로 그 상태를 살린다.
     * 그 밖의 예외는 원인을 로그로 남기고 500으로 응답한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse springError
                && springError.getStatusCode().is4xxClientError()) {
            return toResponse(toErrorCode(springError.getStatusCode()));
        }
        log.error("처리하지 못한 예외", e);
        return toResponse(GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ErrorCode toErrorCode(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> GlobalErrorCode.NOT_FOUND;
            case 405 -> GlobalErrorCode.METHOD_NOT_ALLOWED;
            case 415 -> GlobalErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> GlobalErrorCode.INVALID_INPUT;
        };
    }

    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }
}
