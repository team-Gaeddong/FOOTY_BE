package com.gaeddong.footy.domain.auth.domain.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Google 로그인 성공 후 프론트에 넘기는 1회용 코드(D-05d). 한 번 꺼내면 삭제된다.
 */
@Repository
@RequiredArgsConstructor
public class LoginCodeRepository {

    private static final String KEY_PREFIX = "login-code:";

    private final StringRedisTemplate redisTemplate;

    public void save(String code, Long memberId, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + code, String.valueOf(memberId), ttl);
    }

    public Optional<Long> consume(String code) {
        return Optional.ofNullable(redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + code))
                .map(Long::valueOf);
    }
}
