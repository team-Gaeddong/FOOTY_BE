package com.gaeddong.footy.global.auth;

import com.gaeddong.footy.global.exception.UnauthenticatedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
