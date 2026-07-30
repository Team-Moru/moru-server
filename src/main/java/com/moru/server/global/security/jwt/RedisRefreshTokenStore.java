package com.moru.server.global.security.jwt;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "moru:auth:refresh:";

    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then
                return 0
            end

            redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> DELETE_IF_MATCHES_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then
                return 0
            end

            return redis.call('DEL', KEYS[1])
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(Long memberId, String tokenHash, Duration ttl) {
        redisTemplate.opsForValue().set(key(memberId), tokenHash, ttl);
    }

    @Override
    public boolean rotate(Long memberId, String currentTokenHash, String nextTokenHash, Duration nextTokenTtl) {
        Long result = redisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(key(memberId)),
                currentTokenHash,
                nextTokenHash,
                String.valueOf(toPositiveMillis(nextTokenTtl))
        );

        return Long.valueOf(1L).equals(result);
    }

    @Override
    public boolean deleteIfMatches(Long memberId, String tokenHash) {
        Long result = redisTemplate.execute(
                DELETE_IF_MATCHES_SCRIPT,
                List.of(key(memberId)),
                tokenHash
        );

        return Long.valueOf(1L).equals(result);
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        redisTemplate.delete(key(memberId));
    }

    private String key(Long memberId) {
        return KEY_PREFIX + memberId;
    }

    private long toPositiveMillis(Duration ttl) {
        long ttlMillis = ttl.toMillis();

        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("Refresh Token TTL은 1밀리초 이상이어야 합니다.");
        }

        return ttlMillis;
    }
}
