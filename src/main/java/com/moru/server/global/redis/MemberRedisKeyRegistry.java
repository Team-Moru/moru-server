package com.moru.server.global.redis;

import java.time.Duration;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberRedisKeyRegistry {

    private static final String INDEX_KEY_PREFIX = "moru:member:";
    private static final String INDEX_KEY_SUFFIX = ":keys";
    private static final Duration INDEX_TTL = Duration.ofDays(31);

    private final StringRedisTemplate redisTemplate;

    public void register(Long memberId, String redisKey) {
        String indexKey = indexKey(memberId);
        redisTemplate.opsForSet().add(indexKey, redisKey);
        redisTemplate.expire(indexKey, INDEX_TTL);
    }

    public Set<String> findRegisteredKeys(Long memberId) {
        Set<String> members = redisTemplate.opsForSet().members(indexKey(memberId));
        return members == null ? Set.of() : members;
    }

    public void deleteIndex(Long memberId) {
        redisTemplate.delete(indexKey(memberId));
    }

    private String indexKey(Long memberId) {
        return INDEX_KEY_PREFIX + memberId + INDEX_KEY_SUFFIX;
    }
}
