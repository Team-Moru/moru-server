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
    private static final int MAX_WINDOW_ATTEMPTS = 3;
    private static final long WINDOW_CHANGED = -1;
    private static final DefaultRedisScript<List> CONSUME = new DefaultRedisScript<>("""
            local now = tonumber(redis.call('TIME')[1])
            local blocked = 0
            for i, key in ipairs(KEYS) do
                local starts = tonumber(ARGV[i * 3 - 1])
                local expires = tonumber(ARGV[i * 3])
                if now < starts or now >= expires then return {now, -1} end
                if tonumber(redis.call('GET', key) or '0') >= tonumber(ARGV[i * 3 - 2]) then
                    blocked = blocked + 2^(i - 1)
                end
            end
            if blocked > 0 then return {now, blocked} end
            for i, key in ipairs(KEYS) do
                if redis.call('INCR', key) == 1 then
                    redis.call('EXPIREAT', key, ARGV[i * 3])
                end
            end
            return {now, 0}
            """, List.class);

    private final StringRedisTemplate redis;

    public void consume(String scope, String identity, Limits personal, Limits global, ErrorStatus personalError) {
        Instant now = Instant.now();
        for (int attempt = 0; attempt < MAX_WINDOW_ATTEMPTS; attempt++) {
            var windows = getWindows(scope, identity, personal, global, personalError, now);
            var result = execute(windows);
            if (result.mask() != WINDOW_CHANGED) {
                rejectIfBlocked(List.copyOf(windows.values()), result, personalError);
                return;
            }
            now = Instant.ofEpochSecond(result.now());
        }
        throw new BusinessException(ErrorStatus.QUOTA_UNAVAILABLE);
    }

    private LinkedHashMap<String, Window> getWindows(String scope, String identity, Limits personal, Limits global,
                                                     ErrorStatus personalError, Instant now) {
        var windows = new LinkedHashMap<String, Window>();
        if (personal != null) {
            addWindows(windows, scope, identity, personal, personalError, now);
        }
        if (global != null) {
            addWindows(windows, scope, "global", global, ErrorStatus.SERVICE_CAPACITY_EXCEEDED, now);
        }
        return windows;
    }

    private QuotaResult execute(LinkedHashMap<String, Window> windows) {
        var args = new String[windows.size() * 3];
        int index = 0;
        for (Window window : windows.values()) {
            args[index++] = Integer.toString(window.limit());
            args[index++] = Long.toString(window.starts());
            args[index++] = Long.toString(window.expires());
        }
        List<?> result;
        try {
            result = redis.execute(CONSUME, List.copyOf(windows.keySet()), (Object[]) args);
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorStatus.QUOTA_UNAVAILABLE);
        }
        if (result == null || result.size() != 2
                || !(result.get(0) instanceof Long now) || !(result.get(1) instanceof Long mask)) {
            throw new BusinessException(ErrorStatus.QUOTA_UNAVAILABLE);
        }
        return new QuotaResult(now, mask);
    }

    private void rejectIfBlocked(List<Window> ordered, QuotaResult result, ErrorStatus personalError) {
        long retry = 0;
        ErrorStatus error = personalError;
        for (int i = 0; i < ordered.size(); i++) {
            if ((result.mask() & (1L << i)) != 0) {
                Window window = ordered.get(i);
                retry = Math.max(retry, window.expires() - result.now());
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
        windows.put(key + ":minute:" + minute, new Window(limits.minute(), minute * 60, (minute + 1) * 60, error));
        if (limits.day() != null) {
            var date = now.atZone(SEOUL).toLocalDate();
            windows.put(key + ":day:" + date,
                    new Window(limits.day(), date.atStartOfDay(SEOUL).toEpochSecond(),
                            date.plusDays(1).atStartOfDay(SEOUL).toEpochSecond(), error));
        }
    }

    public record Limits(int minute, Integer day) {
    }

    private record Window(int limit, long starts, long expires, ErrorStatus error) {
    }

    private record QuotaResult(long now, long mask) {
    }
}
