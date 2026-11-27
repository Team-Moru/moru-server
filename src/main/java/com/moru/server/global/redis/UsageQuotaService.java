package com.moru.server.global.redis;

import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.moru.server.global.exception.BusinessException;
import com.moru.server.global.exception.QuotaExceededException;
import com.moru.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsageQuotaService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>("""
            local blocked = 0
            for i, key in ipairs(KEYS) do
                if tonumber(redis.call('GET', key) or '0') >= tonumber(ARGV[i * 2 - 1]) then
                    blocked = blocked + 2^(i - 1)
                end
            end
            if blocked > 0 then return blocked end
            for i, key in ipairs(KEYS) do
                if redis.call('INCR', key) == 1 then
                    redis.call('EXPIREAT', key, ARGV[i * 2])
                end
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;

    public void consume(String scope, String identity, Limits personal, Limits global, ErrorStatus personalError) {
        Instant now = Instant.now();
        var windows = new LinkedHashMap<String, Window>();
        if (personal != null) {
            addWindows(windows, scope, identity, personal, personalError, now);
        }
        if (global != null) {
            addWindows(windows, scope, "global", global, ErrorStatus.SERVICE_CAPACITY_EXCEEDED, now);
        }
        List<Window> ordered = List.copyOf(windows.values());
        String[] args = new String[ordered.size() * 2];
        for (int i = 0; i < ordered.size(); i++) {
            args[2 * i] = Integer.toString(ordered.get(i).limit());
            args[2 * i + 1] = Long.toString(ordered.get(i).expires());
        }
        Long mask;
        try {
            mask = redis.execute(CONSUME, List.copyOf(windows.keySet()), (Object[]) args);
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorStatus.QUOTA_UNAVAILABLE);
        }
        if (mask == null) {
            throw new BusinessException(ErrorStatus.QUOTA_UNAVAILABLE);
        }
        long retry = 0;
        ErrorStatus error = personalError;
        for (int i = 0; i < ordered.size(); i++) {
            if ((mask & (1L << i)) != 0) {
                Window window = ordered.get(i);
                retry = Math.max(retry, window.expires() - now.getEpochSecond());
                if (window.error() == ErrorStatus.SERVICE_CAPACITY_EXCEEDED) {
                    error = window.error();
                }
            }
        }
        if (retry > 0) {
            throw new QuotaExceededException(error, retry);
        }
    }

    private void addWindows(LinkedHashMap<String, Window> windows, String scope, String identity,
                            Limits limits, ErrorStatus error, Instant now) {
        String key = "v2:quota:{" + scope + "}:" + identity;
        long minute = Math.floorDiv(now.getEpochSecond(), 60);
        windows.put(key + ":minute:" + minute, new Window(limits.minute(), (minute + 1) * 60, error));
        if (limits.day() != null) {
            var date = now.atZone(SEOUL).toLocalDate();
            windows.put(key + ":day:" + date,
                    new Window(limits.day(), date.plusDays(1).atStartOfDay(SEOUL).toEpochSecond(), error));
        }
    }

    public record Limits(int minute, Integer day) {
    }

    private record Window(int limit, long expires, ErrorStatus error) {
    }
}
