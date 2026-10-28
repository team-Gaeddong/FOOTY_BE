package com.gaeddong.footy.global.security.oauth;

import com.gaeddong.footy.domain.auth.service.AuthService;
import com.gaeddong.footy.global.security.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class OAuth2LoginSuccessHandlerTest {

    private static final String REDIRECT_URI = "http://localhost:3000/login/callback";

    private final AuthService authService = mock(AuthService.class);
    private final OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(authService,
            new AuthProperties(REDIRECT_URI, Duration.ofMinutes(1)));

    private static TestingAuthenticationToken googleLogin() {
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("sub", "google-sub-1", "email", "player@gmail.com"));
        return new TestingAuthenticationToken(new DefaultOidcUser(List.of(), idToken), null);
    }

    @Test
    void 로그인_코드를_붙여_프론트로_리다이렉트한다() throws Exception {
        given(authService.issueLoginCode("google-sub-1", "player@gmail.com")).willReturn("code-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, googleLogin());

        assertThat(response.getRedirectedUrl()).isEqualTo(REDIRECT_URI + "?code=code-1");
    }

    @Test
    void 코드_발급이_실패하면_에러로_리다이렉트한다() throws Exception {
        given(authService.issueLoginCode("google-sub-1", "player@gmail.com"))
                .willThrow(new DataIntegrityViolationException("uk_member_email"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, googleLogin());

        assertThat(response.getRedirectedUrl()).isEqualTo(REDIRECT_URI + "?error=login_failed");
    }
}
