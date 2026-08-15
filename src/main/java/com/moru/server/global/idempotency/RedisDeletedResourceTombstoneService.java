package com.moru.server.global.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.moru.server.global.redis.MemberRedisKeyRegistry;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisDeletedResourceTombstoneService implements DeletedResourceTombstoneService {

    private final StringRedisTemplate redisTemplate;
    private final MemberRedisKeyRegistry keyRegistry;

    // 재전송 idempotent 처리를 보장할 최대 기간. 이 기간이 지난 뒤의 재전송은
    // "존재한 적 없음"과 동일하게 취급됨 (실질적으로 문제되지 않는 트레이드오프)
    private static final Duration TOMBSTONE_TTL = Duration.ofDays(30);
    private static final String KEY_PREFIX = "moru:deleted:";

    @Override
    public void markDeleted(String resourceType, Long resourceId, Long ownerId) {
        String key = buildKey(resourceType, resourceId);
        redisTemplate.opsForValue().set(key, String.valueOf(ownerId), TOMBSTONE_TTL);
        keyRegistry.register(ownerId, key);
    }

    @Override
    public boolean wasDeletedBy(String resourceType, Long resourceId, Long ownerId) {
        String key = buildKey(resourceType, resourceId);
        String storedOwnerId = redisTemplate.opsForValue().get(key);
        return storedOwnerId != null && storedOwnerId.equals(String.valueOf(ownerId));
    }

    private String buildKey(String resourceType, Long resourceId) {
        return KEY_PREFIX + resourceType + ":" + resourceId;
    }
}
