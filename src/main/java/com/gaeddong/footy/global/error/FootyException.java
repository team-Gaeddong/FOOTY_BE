package com.gaeddong.footy.global.error;

import lombok.Getter;

@Getter
public class FootyException extends RuntimeException {

    private final ErrorCode errorCode;

    public FootyException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
