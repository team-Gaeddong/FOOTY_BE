package com.gaeddong.footy.global.auth;

/** 인증 계층이 현재 요청의 로그인 회원 ID를 제공하는 경계다. */
public interface CurrentMemberIdProvider {

    Long currentMemberId();
}
