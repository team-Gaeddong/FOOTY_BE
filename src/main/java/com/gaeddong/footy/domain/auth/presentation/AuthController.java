package com.gaeddong.footy.domain.auth.presentation;

import com.gaeddong.footy.domain.auth.presentation.dto.request.LoginCodeRequest;
import com.gaeddong.footy.domain.auth.presentation.dto.request.ReissueRequest;
import com.gaeddong.footy.domain.auth.presentation.dto.response.TokenResponse;
import com.gaeddong.footy.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Google 로그인 시작(GET /oauth2/authorization/google)과 콜백은 Spring Security가 처리한다.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/token")
    public TokenResponse exchangeLoginCode(@RequestBody LoginCodeRequest request) {
        return authService.exchangeLoginCode(request.code());
    }

    @PostMapping("/reissue")
    public TokenResponse reissue(@RequestBody ReissueRequest request) {
        return authService.reissue(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        authService.logout((Long) authentication.getPrincipal(), (String) authentication.getCredentials());
        return ResponseEntity.noContent().build();
    }
}
