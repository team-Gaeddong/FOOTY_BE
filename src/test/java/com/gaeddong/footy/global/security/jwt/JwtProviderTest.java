package com.gaeddong.footy.global.security.jwt;

import com.gaeddong.footy.domain.auth.exception.AuthErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final Instant NOW = Instant.parse("2026-10-21T03:00:00Z");
    private static final JwtProperties PROPERTIES = new JwtProperties(
            "test-jwt-secret-key-must-be-at-least-32-bytes", Duration.ofMinutes(30), Duration.ofDays(14));

    private static JwtProvider providerAt(Instant instant) {
        return new JwtProvider(PROPERTIES, Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    void 발급한_토큰에서_회원_ID를_꺼낸다() {
        JwtProvider provider = providerAt(NOW);

        assertThat(provider.getMemberId(provider.createAccessToken(1L), TokenType.ACCESS)).isEqualTo(1L);
        assertThat(provider.getMemberId(provider.createRefreshToken(1L), TokenType.REFRESH)).isEqualTo(1L);
    }

    @Test
    void 같은_회원에게_연달아_발급해도_토큰이_다르다() {
        JwtProvider provider = providerAt(NOW);

        assertThat(provider.createRefreshToken(1L)).isNotEqualTo(provider.createRefreshToken(1L));
    }

    @Test
    void refresh_토큰은_access로_쓸_수_없다() {
        JwtProvider provider = providerAt(NOW);
        String refreshToken = provider.createRefreshToken(1L);

        assertThatThrownBy(() -> provider.getMemberId(refreshToken, TokenType.ACCESS))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(AuthErrorCode.INVALID_TOKEN);
    }

    @Test
    void 만료된_토큰은_거부한다() {
        String accessToken = providerAt(NOW).createAccessToken(1L);
        JwtProvider later = providerAt(NOW.plus(Duration.ofMinutes(31)));

        assertThatThrownBy(() -> later.getMemberId(accessToken, TokenType.ACCESS))
                .isInstanceOf(FootyException.class)
                .extracting("errorCode").isEqualTo(AuthErrorCode.EXPIRED_TOKEN);
    }

    @Test
    void 다른_키로_서명했거나_형식이_잘못된_토큰은_거부한다() {
        JwtProvider other = new JwtProvider(
                new JwtProperties("another-secret-key-must-be-at-least-32-bytes", Duration.ofMinutes(30), Duration.ofDays(14)),
                Clock.fixed(NOW, ZoneOffset.UTC));
        String forged = other.createAccessToken(1L);
        JwtProvider provider = providerAt(NOW);

        assertThatThrownBy(() -> provider.getMemberId(forged, TokenType.ACCESS))
                .extracting("errorCode").isEqualTo(AuthErrorCode.INVALID_TOKEN);
        assertThatThrownBy(() -> provider.getMemberId("not-a-jwt", TokenType.ACCESS))
                .extracting("errorCode").isEqualTo(AuthErrorCode.INVALID_TOKEN);
    }

    @Test
    void 남은_만료_시간을_계산한다() {
        String accessToken = providerAt(NOW).createAccessToken(1L);

        assertThat(providerAt(NOW.plus(Duration.ofMinutes(10))).getRemainingTime(accessToken))
                .isEqualTo(Duration.ofMinutes(20));
    }
}
