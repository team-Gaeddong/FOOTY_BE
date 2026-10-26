package com.gaeddong.footy.domain.game.exception;

import com.gaeddong.footy.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GameErrorCode implements ErrorCode {

    NOT_GAME_HOST(HttpStatus.FORBIDDEN, "GAME_403_1", "파티장만 할 수 있습니다."),
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "GAME_404_1", "경기를 찾을 수 없습니다."),
    GAME_PLAYER_NOT_FOUND(HttpStatus.NOT_FOUND, "GAME_404_2", "경기 참가자를 찾을 수 없습니다."),
    GAME_NOT_RECRUITING(HttpStatus.CONFLICT, "GAME_409_1", "모집 중인 경기에서만 할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
