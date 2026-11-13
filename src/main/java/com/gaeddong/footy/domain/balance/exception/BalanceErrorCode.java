package com.gaeddong.footy.domain.balance.exception;

import com.gaeddong.footy.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BalanceErrorCode implements ErrorCode {

    DUPLICATE_REASON(HttpStatus.BAD_REQUEST, "BALANCE_400_1", "같은 사유를 중복해서 보낼 수 없습니다."),
    NOT_GAME_PLAYER(HttpStatus.FORBIDDEN, "BALANCE_403_1", "경기에 참가한 회원만 밸런스를 평가할 수 있습니다."),
    NOT_GAME_PARTICIPANT(HttpStatus.FORBIDDEN, "BALANCE_403_2", "파티장과 경기 참가자만 밸런스 결과를 조회할 수 있습니다."),
    GAME_NOT_FINISHED(HttpStatus.CONFLICT, "BALANCE_409_1", "종료된 경기만 밸런스를 평가할 수 있습니다."),
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "BALANCE_409_2", "이미 밸런스를 평가한 경기입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
