package com.gaeddong.footy.global.security.oauth;

import com.gaeddong.footy.global.security.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    static final String LOGIN_FAILED = "login_failed";

    private final AuthProperties authProperties;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("Google 로그인 실패: {}", exception.getMessage());
        String redirectUri = UriComponentsBuilder.fromUriString(authProperties.redirectUri())
                .queryParam("error", LOGIN_FAILED)
                .build()
                .toUriString();
        response.sendRedirect(redirectUri);
    }
}
