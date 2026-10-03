package com.chuanqing.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OutboxIdempotencyService {

    private static final Duration COMPLETED_TTL = Duration.ofDays(30);

    private final StringRedisTemplate redis;
    private final MeterRegistry meterRegistry;

    public boolean execute(String consumer, Long eventId, Runnable processor) {
        String completedKey = eventId == null ? null : "dedup:outbox:" + consumer + ":" + eventId;
        if (completedKey != null && Boolean.TRUE.equals(redis.hasKey(completedKey))) {
            meterRegistry.counter("zhiguang.outbox.consumer.duplicate", "consumer", consumer).increment();
            return false;
        }

        try {
            processor.run();
            if (completedKey != null) {
                redis.opsForValue().set(completedKey, "done", COMPLETED_TTL);
            }
            meterRegistry.counter("zhiguang.outbox.consumer.processed", "consumer", consumer).increment();
            return true;
        } catch (RuntimeException exception) {
            meterRegistry.counter("zhiguang.outbox.consumer.failed", "consumer", consumer).increment();
            throw exception;
        }
    }
}
