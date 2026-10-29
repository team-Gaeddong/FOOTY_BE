package com.gaeddong.footy.domain.lineup.exception;

import com.gaeddong.footy.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LineupErrorCode implements ErrorCode {

    DUPLICATE_PLACEMENT(HttpStatus.BAD_REQUEST, "LINEUP_400_1", "같은 선수를 중복해서 배치할 수 없습니다."),
    NOT_TEAM_MEMBER(HttpStatus.BAD_REQUEST, "LINEUP_400_2", "팀에 속하지 않은 선수는 배치할 수 없습니다."),
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "LINEUP_404_1", "팀을 찾을 수 없습니다."),
    LINEUP_ALREADY_EXISTS(HttpStatus.CONFLICT, "LINEUP_409_1", "이미 팀을 짠 경기입니다."),
    UNRATED_PLAYER_EXISTS(HttpStatus.CONFLICT, "LINEUP_409_2", "실력 점수가 없는 선수가 있습니다."),
    NOT_ENOUGH_PLAYERS(HttpStatus.CONFLICT, "LINEUP_409_3", "팀을 짜려면 선수가 2명 이상이어야 합니다."),
    LINEUP_CONFIRMED(HttpStatus.CONFLICT, "LINEUP_409_4", "확정된 팀은 수정할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
