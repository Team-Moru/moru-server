package com.moru.server.domain.member.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.moru.server.domain.member.entity.enums.LoginType;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MemberWithdrawalLock {

    private static final String KEY_PREFIX = "moru:member:";
    private static final String KEY_SUFFIX = ":withdrawal-lock";
    private static final String SOCIAL_KEY_PREFIX = "moru:social-identity:";
    private static final Duration DEFAULT_LOCK_TTL = Duration.ofSeconds(60);
    private static final Duration DEFAULT_RENEW_INTERVAL = Duration.ofSeconds(20);

    private static final DefaultRedisScript<Long> RELEASE_IF_OWNER_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private static final DefaultRedisScript<Long> RENEW_IF_OWNER_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ScheduledExecutorService renewalExecutor;
    private final ConcurrentMap<String, ScheduledFuture<?>> renewalTasks = new ConcurrentHashMap<>();
    private final Duration lockTtl;
    private final Duration renewInterval;

    @Autowired
    public MemberWithdrawalLock(StringRedisTemplate redisTemplate) {
        this(redisTemplate, DEFAULT_LOCK_TTL, DEFAULT_RENEW_INTERVAL);
    }

    MemberWithdrawalLock(
            StringRedisTemplate redisTemplate,
            Duration lockTtl,
            Duration renewInterval
    ) {
        this.redisTemplate = redisTemplate;
        this.lockTtl = lockTtl;
        this.renewInterval = renewInterval;
        this.renewalExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "member-withdrawal-lock-renewer");
            thread.setDaemon(true);
            return thread;
        });
    }

    public Optional<String> tryAcquire(Long memberId) {
        return tryAcquireKey(key(memberId));
    }

    public Optional<String> tryAcquireSocialIdentity(LoginType loginType, String oauthId) {
        return tryAcquireKey(socialIdentityKey(loginType, oauthId));
    }

    private Optional<String> tryAcquireKey(String redisKey) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, token, lockTtl);
        if (!Boolean.TRUE.equals(acquired)) {
            return Optional.empty();
        }

        startRenewal(redisKey, token);
        return Optional.of(token);
    }

    public void release(Long memberId, String token) {
        releaseKey(key(memberId), token);
    }

    public void releaseSocialIdentity(LoginType loginType, String oauthId, String token) {
        releaseKey(socialIdentityKey(loginType, oauthId), token);
    }

    private void releaseKey(String redisKey, String token) {
        cancelRenewal(redisKey, token);
        try {
            redisTemplate.execute(RELEASE_IF_OWNER_SCRIPT, List.of(redisKey), token);
        } catch (RuntimeException exception) {
            log.warn("회원 잠금 해제에 실패했습니다. TTL 만료 후 자동 해제됩니다.");
        }
    }

    public boolean isLocked(Long memberId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(memberId)));
    }

    private String key(Long memberId) {
        return KEY_PREFIX + memberId + KEY_SUFFIX;
    }

    private String socialIdentityKey(LoginType loginType, String oauthId) {
        return SOCIAL_KEY_PREFIX
                + loginType.name().toLowerCase()
                + ":"
                + DigestUtils.sha256Hex(oauthId);
    }

    private void startRenewal(String redisKey, String token) {
        String renewalId = renewalId(redisKey, token);
        ScheduledFuture<?> task = renewalExecutor.scheduleAtFixedRate(
                () -> renew(redisKey, token, renewalId),
                renewInterval.toMillis(),
                renewInterval.toMillis(),
                TimeUnit.MILLISECONDS
        );
        renewalTasks.put(renewalId, task);
    }

    private void renew(String redisKey, String token, String renewalId) {
        try {
            Long renewed = redisTemplate.execute(
                    RENEW_IF_OWNER_SCRIPT,
                    List.of(redisKey),
                    token,
                    String.valueOf(lockTtl.toMillis())
            );
            if (!Long.valueOf(1L).equals(renewed)) {
                ScheduledFuture<?> task = renewalTasks.remove(renewalId);
                if (task != null) {
                    task.cancel(false);
                }
            }
        } catch (RuntimeException exception) {
            log.warn("회원탈퇴 잠금 갱신에 실패했습니다. 다음 갱신 주기에 재시도합니다.");
        }
    }

    private void cancelRenewal(String redisKey, String token) {
        ScheduledFuture<?> task = renewalTasks.remove(renewalId(redisKey, token));
        if (task != null) {
            task.cancel(false);
        }
    }

    private String renewalId(String redisKey, String token) {
        return redisKey + ":" + token;
    }

    @PreDestroy
    void shutDownRenewalExecutor() {
        renewalTasks.values().forEach(task -> task.cancel(false));
        renewalTasks.clear();
        renewalExecutor.shutdownNow();
    }
}
