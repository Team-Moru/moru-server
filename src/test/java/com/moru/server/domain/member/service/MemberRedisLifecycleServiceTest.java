package com.moru.server.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.moru.server.global.redis.MemberRedisKeyRegistry;
import com.moru.server.global.security.jwt.RedisRefreshTokenStore;

@Testcontainers(disabledWithoutDocker = true)
class MemberRedisLifecycleServiceTest {

    private static final int REDIS_PORT = 6379;

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(REDIS_PORT);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    private MemberRedisDataCleaner cleaner;
    private MemberWithdrawalLock withdrawalLock;
    private MemberRedisKeyRegistry keyRegistry;

    @BeforeAll
    static void setUpRedis() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(REDIS_PORT));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void tearDownRedis() {
        connectionFactory.destroy();
    }

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        keyRegistry = new MemberRedisKeyRegistry(redisTemplate);
        RedisRefreshTokenStore refreshTokenStore = new RedisRefreshTokenStore(redisTemplate);
        cleaner = new MemberRedisDataCleaner(redisTemplate, keyRegistry, refreshTokenStore);
        withdrawalLock = new MemberWithdrawalLock(redisTemplate);
    }

    @Test
    void clearsRegisteredAndLegacyMemberDataOnly() {
        Long memberId = 10L;
        RedisRefreshTokenStore refreshTokenStore = new RedisRefreshTokenStore(redisTemplate);
        refreshTokenStore.save(memberId, "refresh-hash", Duration.ofMinutes(10));

        String registeredKey = "moru:member-cache:10";
        redisTemplate.opsForValue().set(registeredKey, "cached-response");
        keyRegistry.register(memberId, registeredKey);

        String legacyIdempotencyKey = "moru:idem:routine-create:10:request-id";
        redisTemplate.opsForValue().set(legacyIdempotencyKey, "cached-response");
        String memberTombstone = "moru:deleted:routine:100";
        String otherMemberTombstone = "moru:deleted:routine:200";
        redisTemplate.opsForValue().set(memberTombstone, "10");
        redisTemplate.opsForValue().set(otherMemberTombstone, "20");

        cleaner.clearMemberData(memberId);

        assertThat(redisTemplate.hasKey("moru:auth:refresh:10")).isFalse();
        assertThat(redisTemplate.hasKey(registeredKey)).isFalse();
        assertThat(redisTemplate.hasKey(legacyIdempotencyKey)).isFalse();
        assertThat(redisTemplate.hasKey(memberTombstone)).isFalse();
        assertThat(redisTemplate.hasKey(otherMemberTombstone)).isTrue();
    }

    @Test
    void allowsOnlyLockOwnerToReleaseWithdrawalLock() {
        String ownerToken = withdrawalLock.tryAcquire(10L).orElseThrow();

        assertThat(withdrawalLock.tryAcquire(10L)).isEmpty();
        withdrawalLock.release(10L, "other-token");
        assertThat(withdrawalLock.isLocked(10L)).isTrue();

        withdrawalLock.release(10L, ownerToken);

        assertThat(withdrawalLock.isLocked(10L)).isFalse();
        assertThat(withdrawalLock.tryAcquire(10L)).isPresent();
    }
}
