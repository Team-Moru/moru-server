package com.moru.server.global.redis;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.moru.server.global.exception.QuotaExceededException;
import com.moru.server.global.redis.UsageQuotaService.Limits;
import com.moru.server.global.response.code.status.ErrorStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsageQuotaServiceTest {
    @Test
    void rebuildsBothWindowsWithRedisTimeAfterAWindowChanges() {
        var redis = mock(StringRedisTemplate.class);
        var service = new UsageQuotaService(redis);
        var newDay = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);
        long midnight = newDay.atStartOfDay(ZoneId.of("Asia/Seoul")).toEpochSecond();
        long nextMinute = midnight + 60;
        long nextDay = midnight + 86_400;
        when(redis.execute(any(), anyList(), any(Object[].class)))
                .thenReturn(List.of(midnight, -1L))
                .thenAnswer(invocation -> {
                    List<String> keys = invocation.getArgument(1);
                    assertThat(keys).containsExactly(
                            "v2:quota:{ai}:installation:minute:" + midnight / 60,
                            "v2:quota:{ai}:installation:day:" + newDay,
                            "v2:quota:{ai}:global:minute:" + midnight / 60,
                            "v2:quota:{ai}:global:day:" + newDay);
                    assertThat((Object[]) invocation.getRawArguments()[2]).containsExactly(
                            "2", Long.toString(midnight), Long.toString(nextMinute),
                            "5", Long.toString(midnight), Long.toString(nextDay),
                            "10", Long.toString(midnight), Long.toString(nextMinute),
                            "300", Long.toString(midnight), Long.toString(nextDay));
                    return List.of(midnight + 1, 6L);
                });

        assertThatThrownBy(() -> service.consume("ai", "installation", new Limits(2, 5),
                new Limits(10, 300), ErrorStatus.AI_ROUTINE_QUOTA_EXCEEDED))
                .isInstanceOfSatisfying(QuotaExceededException.class, e -> {
                    assertThat(e.getError()).isEqualTo(ErrorStatus.SERVICE_CAPACITY_EXCEEDED);
                    assertThat(e.getRetryAfter()).isEqualTo(86_399);
                });
    }
}
