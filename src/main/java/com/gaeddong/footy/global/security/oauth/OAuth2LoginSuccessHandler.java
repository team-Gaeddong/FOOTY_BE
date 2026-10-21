package com.gaeddong.footy.global.security.oauth;

import com.gaeddong.footy.domain.auth.service.AuthService;
import com.gaeddong.footy.global.security.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Google 콜백 성공 시 1회용 코드를 붙여 프론트로 리다이렉트한다(D-05d). 토큰은 URL에 싣지 않는다.
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final AuthProperties authProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OidcUser user = (OidcUser) authentication.getPrincipal();
        String code = authService.issueLoginCode(user.getSubject(), user.getEmail());

        String redirectUri = UriComponentsBuilder.fromUriString(authProperties.redirectUri())
                .queryParam("code", code)
                .build()
                .toUriString();
        response.sendRedirect(redirectUri);
    }
}
