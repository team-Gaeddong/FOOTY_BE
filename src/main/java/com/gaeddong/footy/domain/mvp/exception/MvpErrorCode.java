package com.gaeddong.footy.domain.mvp.exception;

import com.gaeddong.footy.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MvpErrorCode implements ErrorCode {

    SELF_VOTE(HttpStatus.BAD_REQUEST, "MVP_400_1", "자기 자신에게는 MVP 투표를 할 수 없습니다."),
    NOT_GAME_PLAYER(HttpStatus.FORBIDDEN, "MVP_403_1", "경기에 참가한 회원만 MVP 투표를 할 수 있습니다."),
    NOT_VOTED(HttpStatus.FORBIDDEN, "MVP_403_2", "MVP 투표를 한 회원만 결과를 조회할 수 있습니다."),
    GAME_NOT_FINISHED(HttpStatus.CONFLICT, "MVP_409_1", "종료된 경기만 MVP 투표를 할 수 있습니다."),
    ALREADY_VOTED(HttpStatus.CONFLICT, "MVP_409_2", "이미 MVP 투표를 한 경기입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
