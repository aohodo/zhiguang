package com.chuanqing.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxIdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SimpleMeterRegistry meterRegistry;
    private OutboxIdempotencyService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new OutboxIdempotencyService(redis, meterRegistry);
    }

    @Test
    void successfulEventIsMarkedCompleted() {
        when(redis.hasKey("dedup:outbox:search:100")).thenReturn(false);
        when(redis.opsForValue()).thenReturn(valueOperations);
        AtomicBoolean processed = new AtomicBoolean();

        assertThat(service.execute("search", 100L, () -> processed.set(true))).isTrue();

        assertThat(processed).isTrue();
        verify(valueOperations).set("dedup:outbox:search:100", "done", Duration.ofDays(30));
        assertThat(meterRegistry.counter("zhiguang.outbox.consumer.processed", "consumer", "search").count())
                .isEqualTo(1.0);
    }

    @Test
    void completedEventIsSkipped() {
        when(redis.hasKey("dedup:outbox:search:100")).thenReturn(true);
        AtomicBoolean processed = new AtomicBoolean();

        assertThat(service.execute("search", 100L, () -> processed.set(true))).isFalse();

        assertThat(processed).isFalse();
        verify(redis, never()).opsForValue();
    }

    @Test
    void failedEventIsNotMarkedCompleted() {
        when(redis.hasKey("dedup:outbox:relation:101")).thenReturn(false);

        assertThatThrownBy(() -> service.execute("relation", 101L,
                () -> { throw new IllegalStateException("temporary failure"); }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("temporary failure");

        verify(redis, never()).opsForValue();
        assertThat(meterRegistry.counter("zhiguang.outbox.consumer.failed", "consumer", "relation").count())
                .isEqualTo(1.0);
    }
}
