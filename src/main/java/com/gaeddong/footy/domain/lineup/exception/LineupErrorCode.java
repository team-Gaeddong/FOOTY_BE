package com.gaeddong.footy.domain.lineup.exception;

import com.gaeddong.footy.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LineupErrorCode implements ErrorCode {

    LINEUP_ALREADY_EXISTS(HttpStatus.CONFLICT, "LINEUP_409_1", "이미 팀을 짠 경기입니다."),
    UNRATED_PLAYER_EXISTS(HttpStatus.CONFLICT, "LINEUP_409_2", "실력 점수가 없는 선수가 있습니다."),
    NOT_ENOUGH_PLAYERS(HttpStatus.CONFLICT, "LINEUP_409_3", "팀을 짜려면 선수가 2명 이상이어야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
