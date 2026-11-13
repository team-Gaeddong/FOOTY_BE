package com.gaeddong.footy.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.util.UriComponentsBuilder;

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

    /**
     * 로그인 결과를 쿼리 파라미터 하나로 붙인 프론트 주소. 성공이면 code, 실패면 error를 붙인다.
     */
    public String redirectUriWith(String name, String value) {
        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam(name, value)
                .build()
                .toUriString();
    }
}
