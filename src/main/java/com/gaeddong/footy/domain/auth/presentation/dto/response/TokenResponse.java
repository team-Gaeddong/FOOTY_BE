package com.gaeddong.footy.domain.auth.presentation.dto.response;

/**
 * onboarded가 false면 프론트는 온보딩 화면으로 보낸다(D-05e).
 */
public record TokenResponse(String accessToken, String refreshToken, boolean onboarded) {
}
