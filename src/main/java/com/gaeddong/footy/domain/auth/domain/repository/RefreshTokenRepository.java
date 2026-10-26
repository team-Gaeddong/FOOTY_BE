package com.gaeddong.footy.domain.auth.domain.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

/**
 * 회원별 현재 유효한 refresh 토큰(D-05b). 회원당 1개만 두고, 재발급하면 덮어쓴다(회전, D-05d).
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh:";

    // 저장된 토큰이 기대값과 같을 때만 새 토큰으로 바꾼다. 비교와 교체를 한 번에 해 동시 재발급을 하나만 통과시킨다.
    private static final RedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
                return 1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public void save(Long memberId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + memberId, refreshToken, ttl);
    }

    /**
     * 저장된 refresh 토큰이 currentToken과 같으면 newToken으로 바꾸고 true를 돌려준다.
     */
    public boolean rotate(Long memberId, String currentToken, String newToken, Duration ttl) {
        Long result = redisTemplate.execute(ROTATE_SCRIPT, List.of(KEY_PREFIX + memberId),
                currentToken, newToken, String.valueOf(ttl.toMillis()));
        return Long.valueOf(1L).equals(result);
    }

    public void deleteByMemberId(Long memberId) {
        redisTemplate.delete(KEY_PREFIX + memberId);
    }
}
