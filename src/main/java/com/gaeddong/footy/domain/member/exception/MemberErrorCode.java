package com.gaeddong.footy.domain.member.exception;

import com.gaeddong.footy.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "MEMBER_400_1", "닉네임은 앞뒤 공백을 제외하고 2~10자여야 합니다."),
    INVALID_POSITIONS(HttpStatus.BAD_REQUEST, "MEMBER_400_2", "포지션은 GK·DF·MF·FW 중 1~4개를 중복 없이 골라야 합니다."),
    ONBOARDING_REQUIRED(HttpStatus.FORBIDDEN, "MEMBER_403_1", "온보딩을 마친 뒤 이용할 수 있습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_404_1", "회원을 찾을 수 없습니다."),
    ALREADY_ONBOARDED(HttpStatus.CONFLICT, "MEMBER_409_1", "이미 온보딩을 마친 회원입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
