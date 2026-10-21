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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AuthServiceTest {

    private static final Duration CODE_TTL = Duration.ofMinutes(1);
    private static final Duration REFRESH_TTL = Duration.ofDays(14);

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final LoginCodeRepository loginCodeRepository = mock(LoginCodeRepository.class);
    private final AccessTokenBlacklistRepository blacklistRepository = mock(AccessTokenBlacklistRepository.class);
    private final JwtProvider jwtProvider = mock(JwtProvider.class);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(memberRepository, refreshTokenRepository, loginCodeRepository,
                blacklistRepository, jwtProvider, new AuthProperties("http://localhost:3000/login/callback", CODE_TTL));
        given(jwtProvider.getRefreshTokenExpiration()).willReturn(REFRESH_TTL);
    }

    private static Member member(Long id) {
        Member member = Member.builder()
                .email("player@gmail.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub-1")
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    @Test
    void 첫_로그인이면_회원을_만들고_코드를_발급한다() {
        given(memberRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-sub-1"))
                .willReturn(Optional.empty());
        given(memberRepository.save(any(Member.class))).willReturn(member(1L));

        String code = authService.issueLoginCode("google-sub-1", "player@gmail.com");

        assertThat(code).isNotBlank();
        verify(memberRepository).save(any(Member.class));
        verify(loginCodeRepository).save(code, 1L, CODE_TTL);
    }

    @Test
    void 가입한_회원이면_새로_만들지_않는다() {
        given(memberRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-sub-1"))
                .willReturn(Optional.of(member(1L)));

        String code = authService.issueLoginCode("google-sub-1", "player@gmail.com");

        verify(memberRepository, never()).save(any());
        verify(loginCodeRepository).save(code, 1L, CODE_TTL);
    }

    @Test
    void 탈퇴한_회원이_다시_로그인하면_복구한다() {
        Member withdrawn = member(1L);
        withdrawn.withdraw(LocalDateTime.of(2026, 10, 20, 12, 0));
        given(memberRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-sub-1"))
                .willReturn(Optional.of(withdrawn));

        authService.issueLoginCode("google-sub-1", "player@gmail.com");

        assertThat(withdrawn.isDeleted()).isFalse();
    }

    @Test
    void 로그인_코드를_토큰으로_교환한다() {
        given(loginCodeRepository.consume("code")).willReturn(Optional.of(1L));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member(1L)));
        given(jwtProvider.createAccessToken(1L)).willReturn("access");
        given(jwtProvider.createRefreshToken(1L)).willReturn("refresh");

        TokenResponse response = authService.exchangeLoginCode("code");

        assertThat(response).isEqualTo(new TokenResponse("access", "refresh", false));
        verify(refreshTokenRepository).save(1L, "refresh", REFRESH_TTL);
    }

    @Test
    void 없거나_이미_쓴_로그인_코드는_거부한다() {
        given(loginCodeRepository.consume("used")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.exchangeLoginCode("used"))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(AuthErrorCode.INVALID_LOGIN_CODE);
    }

    @Test
    void 빈_로그인_코드는_입력_오류다() {
        assertThatThrownBy(() -> authService.exchangeLoginCode(" "))
                .extracting("errorCode").isEqualTo(GlobalErrorCode.INVALID_INPUT);
    }

    @Test
    void 재발급하면_refresh_토큰도_바뀐다() {
        given(jwtProvider.getMemberId("old-refresh", TokenType.REFRESH)).willReturn(1L);
        given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.of("old-refresh"));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member(1L)));
        given(jwtProvider.createAccessToken(1L)).willReturn("new-access");
        given(jwtProvider.createRefreshToken(1L)).willReturn("new-refresh");

        TokenResponse response = authService.reissue("old-refresh");

        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenRepository).save(1L, "new-refresh", REFRESH_TTL);
    }

    @Test
    void 저장된_것과_다른_refresh_토큰은_거부한다() {
        given(jwtProvider.getMemberId("stale-refresh", TokenType.REFRESH)).willReturn(1L);
        given(refreshTokenRepository.findByMemberId(1L)).willReturn(Optional.of("current-refresh"));

        assertThatThrownBy(() -> authService.reissue("stale-refresh"))
                .extracting("errorCode").isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
        verify(refreshTokenRepository, never()).save(any(), anyString(), any());
    }

    @Test
    void 로그아웃하면_refresh를_지우고_access를_블랙리스트에_넣는다() {
        given(jwtProvider.getRemainingTime("access")).willReturn(Duration.ofMinutes(20));

        authService.logout(1L, "access");

        verify(refreshTokenRepository).deleteByMemberId(1L);
        verify(blacklistRepository).save(eq("access"), eq(Duration.ofMinutes(20)));
    }
}
