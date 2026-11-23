package com.gaeddong.footy.domain.game.service;

/**
 * 경기 목록을 요청자와의 관계로 좁히는 조건(D-37).
 */
public enum GameListRole {
    // 요청자가 파티장인 경기
    HOST,
    // 요청자가 참가 선수인 경기. 파티장은 자동 참가하므로 직접 만든 경기도 포함된다
    JOINED
}
