package com.gaeddong.footy.domain.auth.service;

import com.gaeddong.footy.domain.auth.domain.repository.AccessTokenBlacklistRepository;
import com.gaeddong.footy.domain.auth.domain.repository.LoginCodeRepository;
import com.gaeddong.footy.domain.auth.domain.repository.RefreshTokenRepository;
import com.gaeddong.footy.domain.auth.exception.AuthErrorCode;
import com.gaeddong.footy.domain.auth.presentation.dto.response.TokenResponse;
import com.gaeddong.footy.domain.member.domain.Member;
import com.gaeddong.footy.domain.member.domain.Provider;
import com.gaeddong.footy.domain.member.domain.repository.MemberRepository;
import com.gaeddong.footy.global.error.FootyException;
import com.gaeddong.footy.global.error.GlobalErrorCode;
import com.gaeddong.footy.global.security.AuthProperties;
import com.gaeddong.footy.global.security.jwt.JwtProvider;
import com.gaeddong.footy.global.security.jwt.TokenType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginCodeRepository loginCodeRepository;
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;
    private final JwtProvider jwtProvider;
    private final AuthProperties authProperties;

    /**
     * Google 로그인 성공 시 회원을 찾거나 만들고(탈퇴 회원은 복구) 1회용 로그인 코드를 발급한다.
     */
    @Transactional
    public String issueLoginCode(String providerId, String email) {
        Member member = memberRepository.findByProviderAndProviderId(Provider.GOOGLE, providerId)
                .orElseGet(() -> memberRepository.save(Member.builder()
                        .email(email)
                        .provider(Provider.GOOGLE)
                        .providerId(providerId)
                        .build()));
        if (member.isDeleted()) {
            member.restore();
        }

        String code = UUID.randomUUID().toString();
        loginCodeRepository.save(code, member.getId(), authProperties.loginCodeExpiration());
        return code;
    }

    @Transactional(readOnly = true)
    public TokenResponse exchangeLoginCode(String code) {
        requireText(code);
        Long memberId = loginCodeRepository.consume(code)
                .orElseThrow(() -> new FootyException(AuthErrorCode.INVALID_LOGIN_CODE));
        Member member = memberRepository.findById(memberId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new FootyException(AuthErrorCode.INVALID_LOGIN_CODE));
        return issueTokens(member);
    }

    /**
     * refresh 토큰을 회전한다. 저장된 토큰과 다르면(이미 쓴 토큰 등) 거부한다.
     */
    @Transactional(readOnly = true)
    public TokenResponse reissue(String refreshToken) {
        requireText(refreshToken);
        Long memberId = jwtProvider.getMemberId(refreshToken, TokenType.REFRESH);
        boolean matches = refreshTokenRepository.findByMemberId(memberId)
                .filter(refreshToken::equals)
                .isPresent();
        if (!matches) {
            throw new FootyException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        Member member = memberRepository.findById(memberId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new FootyException(AuthErrorCode.INVALID_REFRESH_TOKEN));
        return issueTokens(member);
    }

    public void logout(Long memberId, String accessToken) {
        refreshTokenRepository.deleteByMemberId(memberId);
        Duration remaining = jwtProvider.getRemainingTime(accessToken);
        if (!remaining.isZero()) {
            accessTokenBlacklistRepository.save(accessToken, remaining);
        }
    }

    private TokenResponse issueTokens(Member member) {
        String accessToken = jwtProvider.createAccessToken(member.getId());
        String refreshToken = jwtProvider.createRefreshToken(member.getId());
        refreshTokenRepository.save(member.getId(), refreshToken, jwtProvider.getRefreshTokenExpiration());
        return new TokenResponse(accessToken, refreshToken, member.isOnboarded());
    }

    private void requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new FootyException(GlobalErrorCode.INVALID_INPUT);
        }
    }
}
