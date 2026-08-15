package com.moru.server.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class MemberWithdrawalLockConcurrencyTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private MemberWithdrawalLock withdrawalLock;

    @AfterEach
    void tearDown() {
        if (withdrawalLock != null) {
            withdrawalLock.shutDownRenewalExecutor();
        }
    }

    @Test
    void renewsAnotherLockWhileOneRedisRenewalCallIsBlocked() throws InterruptedException {
        CountDownLatch firstRenewalStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstRenewal = new CountDownLatch(1);
        CountDownLatch secondLockRenewed = new CountDownLatch(1);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        doAnswer(invocation -> {
            List<String> keys = invocation.getArgument(1);
            String key = keys.getFirst();
            if (key.contains("moru:member:1:")) {
                firstRenewalStarted.countDown();
                releaseFirstRenewal.await(1, TimeUnit.SECONDS);
            }
            if (key.contains("moru:member:2:")) {
                secondLockRenewed.countDown();
            }
            return 1L;
        }).when(redisTemplate).execute(
                any(),
                anyList(),
                any(Object[].class)
        );

        withdrawalLock = new MemberWithdrawalLock(
                redisTemplate,
                Duration.ofSeconds(1),
                Duration.ofMillis(50),
                2
        );
        withdrawalLock.tryAcquire(1L).orElseThrow();
        withdrawalLock.tryAcquire(2L).orElseThrow();

        try {
            assertThat(firstRenewalStarted.await(500, TimeUnit.MILLISECONDS)).isTrue();
            assertThat(secondLockRenewed.await(300, TimeUnit.MILLISECONDS)).isTrue();
        } finally {
            releaseFirstRenewal.countDown();
        }
    }

    @Test
    void rejectsSingleThreadRenewalPool() {
        assertThatThrownBy(() -> new MemberWithdrawalLock(
                redisTemplate,
                Duration.ofSeconds(60),
                Duration.ofSeconds(20),
                1
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
