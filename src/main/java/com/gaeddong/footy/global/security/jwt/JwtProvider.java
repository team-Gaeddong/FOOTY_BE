package com.gaeddong.footy.global.security.jwt;

import com.gaeddong.footy.domain.auth.exception.AuthErrorCode;
import com.gaeddong.footy.global.error.FootyException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private static final String TYPE_CLAIM = "type";

    private final JwtProperties properties;
    private final SecretKey key;
    private final Clock clock;

    public JwtProvider(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    public String createAccessToken(Long memberId) {
        return createToken(memberId, TokenType.ACCESS, properties.accessTokenExpiration());
    }

    public String createRefreshToken(Long memberId) {
        return createToken(memberId, TokenType.REFRESH, properties.refreshTokenExpiration());
    }

    public Duration getRefreshTokenExpiration() {
        return properties.refreshTokenExpiration();
    }

    /**
     * 서명·만료·토큰 종류를 검증하고 회원 ID를 돌려준다.
     */
    public Long getMemberId(String token, TokenType expectedType) {
        Claims claims = parse(token);
        if (!expectedType.name().equals(claims.get(TYPE_CLAIM, String.class))) {
            throw new FootyException(AuthErrorCode.INVALID_TOKEN);
        }
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 만료까지 남은 시간. 이미 만료됐으면 0이다.
     */
    public Duration getRemainingTime(String token) {
        Instant expiration = parse(token).getExpiration().toInstant();
        Duration remaining = Duration.between(clock.instant(), expiration);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    private String createToken(Long memberId, TokenType type, Duration expiration) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .id(UUID.randomUUID().toString())
                .claim(TYPE_CLAIM, type.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new FootyException(AuthErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new FootyException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}
