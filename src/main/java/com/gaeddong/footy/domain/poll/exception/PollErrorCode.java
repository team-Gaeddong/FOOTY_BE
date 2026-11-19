package com.gaeddong.footy.domain.poll.exception;

import com.gaeddong.footy.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PollErrorCode implements ErrorCode {

    INVALID_OPTIONS(HttpStatus.BAD_REQUEST, "POLL_400_1", "선택지는 2~10개이고 서로 달라야 합니다."),
    PAST_DEADLINE(HttpStatus.BAD_REQUEST, "POLL_400_2", "마감 일시는 현재 이후여야 합니다."),
    MULTIPLE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "POLL_400_3", "하나만 고를 수 있는 투표입니다."),
    DUPLICATE_OPTION(HttpStatus.BAD_REQUEST, "POLL_400_4", "같은 선택지를 중복해서 보낼 수 없습니다."),
    NOT_GAME_PARTICIPANT(HttpStatus.FORBIDDEN, "POLL_403_1", "경기 참가자만 투표할 수 있습니다."),
    POLL_NOT_FOUND(HttpStatus.NOT_FOUND, "POLL_404_1", "투표를 찾을 수 없습니다."),
    OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "POLL_404_2", "이 투표의 선택지가 아닙니다."),
    POLL_CLOSED(HttpStatus.CONFLICT, "POLL_409_1", "마감된 투표입니다."),
    CONCURRENT_ANSWER(HttpStatus.CONFLICT, "POLL_409_2", "동시에 다른 응답이 처리되었습니다. 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
