package com.gaeddong.footy.domain.game.exception;

import com.gaeddong.footy.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GameErrorCode implements ErrorCode {

    DUPLICATE_GAME_PLAYER(HttpStatus.BAD_REQUEST, "GAME_400_1", "같은 선수를 중복해서 보낼 수 없습니다."),
    INVALID_MAX_PLAYERS(HttpStatus.BAD_REQUEST, "GAME_400_2", "최대 인원은 2~30명이어야 합니다."),
    PAST_PLAY_DATE(HttpStatus.BAD_REQUEST, "GAME_400_3", "경기 일시는 현재 이후여야 합니다."),
    INVALID_TEAM_COUNT(HttpStatus.BAD_REQUEST, "GAME_400_4", "팀 수는 2 이상이고 최대 인원 이하여야 합니다."),
    NOT_GAME_HOST(HttpStatus.FORBIDDEN, "GAME_403_1", "파티장만 할 수 있습니다."),
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "GAME_404_1", "경기를 찾을 수 없습니다."),
    GAME_PLAYER_NOT_FOUND(HttpStatus.NOT_FOUND, "GAME_404_2", "경기 참가자를 찾을 수 없습니다."),
    GAME_NOT_RECRUITING(HttpStatus.CONFLICT, "GAME_409_1", "모집 중인 경기에서만 할 수 있습니다."),
    GAME_NOT_FINISHED(HttpStatus.CONFLICT, "GAME_409_2", "종료된 경기만 재경기를 만들 수 있습니다."),
    ALREADY_JOINED(HttpStatus.CONFLICT, "GAME_409_3", "이미 참여한 경기입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
