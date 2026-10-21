package com.gaeddong.footy.global.auth;

import com.gaeddong.footy.global.exception.UnauthenticatedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 인증 구현 전에는 모든 HTTP 요청을 401로 막는다.
 * 로그인 기능이 완성되면 CurrentMemberIdProvider 구현체만 JWT 기반으로 교체한다.
 */
@Configuration
public class AuthenticationProviderConfig {

    @Bean
    @ConditionalOnMissingBean(CurrentMemberIdProvider.class)
    CurrentMemberIdProvider currentMemberIdProvider() {
        return () -> {
            throw new UnauthenticatedException();
        };
    }
}
