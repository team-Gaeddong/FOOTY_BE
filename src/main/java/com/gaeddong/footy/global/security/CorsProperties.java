package com.gaeddong.footy.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @param allowedOrigins 다른 origin에서 API를 호출할 프론트 주소 목록(D-15). 비어 있으면 다른 origin을 허용하지 않는다
 */
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
}
