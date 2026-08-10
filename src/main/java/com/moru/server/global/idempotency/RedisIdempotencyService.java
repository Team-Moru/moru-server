package com.moru.server.global.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;


@Slf4j
@Service
@RequiredArgsConstructor
public class RedisIdempotencyService implements IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Duration PROCESSING_TTL = Duration.ofSeconds(30);
    private static final Duration COMPLETED_TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "moru:idem:";

    private static final DefaultRedisScript<Long> COMPLETE_IF_OWNER_SCRIPT = new DefaultRedisScript<>("""
        local current = redis.call('GET', KEYS[1])
        if current == false then
            return 0
        end
        local ok, decoded = pcall(cjson.decode, current)
        if not ok or decoded.token ~= ARGV[1] or decoded.status ~= 'PROCESSING' then
            return 0
        end
        redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
        return 1
        """, Long.class);

    private static final DefaultRedisScript<Long> RELEASE_IF_PROCESSING_SCRIPT = new DefaultRedisScript<>("""
        local current = redis.call('GET', KEYS[1])
        if current == false then
            return 0
        end
        local ok, decoded = pcall(cjson.decode, current)
        if not ok or decoded.token ~= ARGV[1] or decoded.status ~= 'PROCESSING' then
            return 0
        end
        return redis.call('DEL', KEYS[1])
        """, Long.class);

    @Override
    public <T> T execute(
            String action,
            Long memberId,
            String idempotencyKey,
            Class<T> responseType,
            Supplier<T> operation
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return operation.get();
        }

        String redisKey = KEY_PREFIX + action + ":" + memberId + ":" + idempotencyKey;
        String token = UUID.randomUUID().toString();

        Envelope processing = new Envelope("PROCESSING", token, null);
        Boolean isFirst = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, writeJson(processing), PROCESSING_TTL);

        if (Boolean.FALSE.equals(isFirst)) {
            Envelope existing = readJson(redisTemplate.opsForValue().get(redisKey));

            if (existing == null) {
                return operation.get();
            }
            if ("COMPLETED".equals(existing.status())) {
                log.info("[Idempotency] 캐시된 응답 재반환. key={}", redisKey);
                return readValue(existing.body(), responseType);
            }
            throw new BusinessException(ErrorStatus.DUPLICATE_REQUEST);
        }

        try {
            T result = operation.get();
            Envelope completed = new Envelope("COMPLETED", token, writeValue(result));
            redisTemplate.execute(
                    COMPLETE_IF_OWNER_SCRIPT,
                    List.of(redisKey),
                    token,
                    writeJson(completed),
                    String.valueOf(COMPLETED_TTL.toSeconds())
            );
            return result;
        } catch (RuntimeException e) {
            redisTemplate.execute(RELEASE_IF_PROCESSING_SCRIPT, List.of(redisKey), token);
            throw e;
        }
    }

    private String writeJson(Envelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new IllegalStateException("idempotency envelope 직렬화 실패", e);
        }
    }

    private Envelope readJson(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, Envelope.class);
        } catch (Exception e) {
            log.warn("[Idempotency] envelope 파싱 실패. json={}", json, e);
            return null;
        }
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("응답 직렬화 실패", e);
        }
    }

    private <T> T readValue(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("캐시된 응답 역직렬화 실패", e);
        }
    }

    private record Envelope(String status, String token, String body) {}
}