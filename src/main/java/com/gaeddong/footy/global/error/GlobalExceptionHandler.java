package com.gaeddong.footy.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FootyException.class)
    public ResponseEntity<ErrorResponse> handleFootyException(FootyException e) {
        return toResponse(e.getErrorCode());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return toResponse(GlobalErrorCode.INVALID_INPUT);
    }

    /**
     * Spring MVC 예외(404·405 등)는 ErrorResponse로 상태를 알려 주므로 그 상태를 살린다.
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
