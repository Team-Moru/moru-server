package com.moru.server.domain.member.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.moru.server.global.redis.MemberRedisKeyRegistry;
import com.moru.server.global.security.jwt.RefreshTokenStore;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberRedisDataCleaner {

    private static final String TOMBSTONE_PATTERN = "moru:deleted:%d:*";

    private final StringRedisTemplate redisTemplate;
    private final MemberRedisKeyRegistry keyRegistry;
    private final RefreshTokenStore refreshTokenStore;

    public void clearMemberData(Long memberId) {
        refreshTokenStore.deleteByMemberId(memberId);

        Set<String> registeredKeys = keyRegistry.findRegisteredKeys(memberId);
        if (!registeredKeys.isEmpty()) {
            redisTemplate.delete(registeredKeys);
        }
        keyRegistry.deleteIndex(memberId);

        deleteKeys(scanKeys(TOMBSTONE_PATTERN.formatted(memberId)));
    }

    private List<String> scanKeys(String pattern) {
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        }
        return keys;
    }

    private void deleteKeys(List<String> keys) {
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
