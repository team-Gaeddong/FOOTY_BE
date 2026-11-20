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
    TEAM_SET_MISMATCH(HttpStatus.BAD_REQUEST, "LINEUP_400_3", "요청한 팀이 현재 라인업의 팀과 다릅니다."),
    PLAYER_MISSING(HttpStatus.BAD_REQUEST, "LINEUP_400_4", "라인업의 모든 선수를 한 팀에 배정해야 합니다."),
    EMPTY_TEAM(HttpStatus.BAD_REQUEST, "LINEUP_400_5", "팀에는 선수가 1명 이상 있어야 합니다."),
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "LINEUP_404_1", "팀을 찾을 수 없습니다."),
    LINEUP_NOT_FOUND(HttpStatus.NOT_FOUND, "LINEUP_404_2", "아직 팀을 짜지 않은 경기입니다."),
    LINEUP_ALREADY_EXISTS(HttpStatus.CONFLICT, "LINEUP_409_1", "이미 팀을 짠 경기입니다."),
    UNRATED_PLAYER_EXISTS(HttpStatus.CONFLICT, "LINEUP_409_2", "실력 점수가 없는 선수가 있습니다."),
    NOT_ENOUGH_PLAYERS(HttpStatus.CONFLICT, "LINEUP_409_3", "팀을 짜려면 선수가 2명 이상이어야 합니다."),
    LINEUP_CONFIRMED(HttpStatus.CONFLICT, "LINEUP_409_4", "확정된 팀은 수정할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
