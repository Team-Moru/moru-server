package com.moru.server.domain.member.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberWithdrawalLock {

    private static final String KEY_PREFIX = "moru:member:";
    private static final String KEY_SUFFIX = ":withdrawal-lock";
    private static final Duration LOCK_TTL = Duration.ofSeconds(60);

    private static final DefaultRedisScript<Long> RELEASE_IF_OWNER_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public Optional<String> tryAcquire(Long memberId) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key(memberId), token, LOCK_TTL);
        return Boolean.TRUE.equals(acquired) ? Optional.of(token) : Optional.empty();
    }

    public void release(Long memberId, String token) {
        redisTemplate.execute(RELEASE_IF_OWNER_SCRIPT, List.of(key(memberId)), token);
    }

    public boolean isLocked(Long memberId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(memberId)));
    }

    private String key(Long memberId) {
        return KEY_PREFIX + memberId + KEY_SUFFIX;
    }
}
