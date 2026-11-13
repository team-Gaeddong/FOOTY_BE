package com.gaeddong.footy.global.security.oauth;

import com.gaeddong.footy.global.security.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    static final String ERROR_PARAM = "error";
    static final String LOGIN_FAILED = "login_failed";

    private final AuthProperties authProperties;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("Google 로그인 실패: {}", exception.getMessage());
        response.sendRedirect(authProperties.redirectUriWith(ERROR_PARAM, LOGIN_FAILED));
    }
}
