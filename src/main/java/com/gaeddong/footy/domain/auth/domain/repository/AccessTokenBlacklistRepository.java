package com.gaeddong.footy.domain.auth.domain.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 로그아웃한 access 토큰. 남은 만료 시간만큼만 보관한다(D-05b).
 */
@Repository
@RequiredArgsConstructor
public class AccessTokenBlacklistRepository {

    private static final String KEY_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void save(String accessToken, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + accessToken, "logout", ttl);
    }

    public boolean contains(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + accessToken));
    }
}
