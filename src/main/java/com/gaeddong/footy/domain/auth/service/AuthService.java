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
        // 코드를 받은 뒤 탈퇴했으면 그 코드는 쓸 수 없다
        Member member = findActiveMember(memberId, AuthErrorCode.INVALID_LOGIN_CODE);
        return issueTokens(member);
    }

    /**
     * refresh 토큰을 회전한다. 저장된 토큰과 비교·교체를 한 번에 해서(Lua) 같은 토큰으로 동시에 요청해도 하나만 통과한다.
     * 저장된 것과 다르면(이미 쓴 토큰 등) 거부한다.
     */
    @Transactional(readOnly = true)
    public TokenResponse reissue(String refreshToken) {
        requireText(refreshToken);
        Long memberId = jwtProvider.getMemberId(refreshToken, TokenType.REFRESH);
        Member member = findActiveMember(memberId, AuthErrorCode.INVALID_REFRESH_TOKEN);

        String newAccessToken = jwtProvider.createAccessToken(memberId);
        String newRefreshToken = jwtProvider.createRefreshToken(memberId);
        boolean rotated = refreshTokenRepository.rotate(memberId, refreshToken, newRefreshToken,
                jwtProvider.getRefreshTokenExpiration());
        if (!rotated) {
            throw new FootyException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        return new TokenResponse(newAccessToken, newRefreshToken, member.isOnboarded());
    }

    /**
     * refresh 토큰을 지우고, access 토큰은 남은 만료 시간만큼 블랙리스트에 둔다(D-05b).
     */
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

    // 없는 회원과 탈퇴한 회원을 구분하지 않고, 인증 흐름마다 정해진 에러로 알린다
    private Member findActiveMember(Long memberId, AuthErrorCode errorCode) {
        return memberRepository.findById(memberId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new FootyException(errorCode));
    }

    private void requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new FootyException(GlobalErrorCode.INVALID_INPUT);
        }
    }
}
