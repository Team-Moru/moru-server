package com.moru.server.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Testcontainers(disabledWithoutDocker = true)
class RedisRefreshTokenStoreTest {

    private static final int REDIS_PORT = 6379;
    private static final String REFRESH_TOKEN_KEY_PREFIX = "moru:auth:refresh:";

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(REDIS_PORT);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static RedisRefreshTokenStore refreshTokenStore;

    @BeforeAll
    static void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(REDIS_PORT));
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        refreshTokenStore = new RedisRefreshTokenStore(redisTemplate);
    }

    @AfterAll
    static void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void expiresRefreshTokenWithItsTtl() throws InterruptedException {
        Long memberId = 1L;

        refreshTokenStore.save(memberId, "token-hash", Duration.ofMillis(150));

        assertThat(redisTemplate.opsForValue().get(key(memberId))).isEqualTo("token-hash");

        waitForExpiration(memberId);

        assertThat(redisTemplate.hasKey(key(memberId))).isFalse();
    }

    @Test
    void allowsOnlyOneConcurrentRefreshTokenRotation() throws Exception {
        Long memberId = 2L;
        refreshTokenStore.save(memberId, "current-token-hash", Duration.ofMinutes(1));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            List<Future<Boolean>> results = List.of(
                    executorService.submit(rotateWhenStarted(ready, start, memberId, "next-token-hash-1")),
                    executorService.submit(rotateWhenStarted(ready, start, memberId, "next-token-hash-2"))
            );

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            long successCount = 0;
            for (Future<Boolean> result : results) {
                if (result.get(5, TimeUnit.SECONDS)) {
                    successCount++;
                }
            }

            assertThat(successCount).isEqualTo(1);
            assertThat(redisTemplate.opsForValue().get(key(memberId)))
                    .isIn("next-token-hash-1", "next-token-hash-2");
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void deletesRefreshTokenOnlyWhenTokenHashMatches() {
        Long memberId = 3L;
        refreshTokenStore.save(memberId, "token-hash", Duration.ofMinutes(1));

        assertThat(refreshTokenStore.deleteIfMatches(memberId, "different-token-hash")).isFalse();
        assertThat(redisTemplate.hasKey(key(memberId))).isTrue();

        assertThat(refreshTokenStore.deleteIfMatches(memberId, "token-hash")).isTrue();
        assertThat(redisTemplate.hasKey(key(memberId))).isFalse();
    }

    private Callable<Boolean> rotateWhenStarted(
            CountDownLatch ready,
            CountDownLatch start,
            Long memberId,
            String nextTokenHash
    ) {
        return () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);

            return refreshTokenStore.rotate(
                    memberId,
                    "current-token-hash",
                    nextTokenHash,
                    Duration.ofMinutes(1)
            );
        };
    }

    private void waitForExpiration(Long memberId) throws InterruptedException {
        for (int attempt = 0; attempt < 100; attempt++) {
            if (Boolean.FALSE.equals(redisTemplate.hasKey(key(memberId)))) {
                return;
            }

            Thread.sleep(20);
        }
    }

    private String key(Long memberId) {
        return REFRESH_TOKEN_KEY_PREFIX + memberId;
    }
}
