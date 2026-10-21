package com.gaeddong.footy.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param redirectUri         Google 로그인 후 돌아갈 프론트 주소(D-05d)
 * @param loginCodeExpiration 1회용 로그인 코드의 유효 시간
 */
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        String redirectUri,
        Duration loginCodeExpiration
) {
}
