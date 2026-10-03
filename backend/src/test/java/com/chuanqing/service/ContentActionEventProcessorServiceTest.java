package com.chuanqing.service;

import com.chuanqing.entity.ContentActionEntity;
import com.chuanqing.entity.ContentActionEventEntity;
import com.chuanqing.mapper.ContentActionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentActionEventProcessorServiceTest {

    @Mock
    private ContentActionMapper actionMapper;
    @Mock
    private CounterService counterService;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private SetOperations<String, String> setOperations;
    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private ContentActionEventProcessorService processor;

    @BeforeEach
    void setUp() {
        processor = new ContentActionEventProcessorService(
                actionMapper, counterService, redis, eventPublisher);
    }

    @Test
    void replayUsesCurrentDatabaseFactAndExactCount() {
        ContentActionEntity current = new ContentActionEntity();
        current.setId(10L);
        current.setActionStatus(1);
        current.setUpdatedAt(LocalDateTime.now());
        when(actionMapper.findByKey(1L, "knowpost", 9L, "like")).thenReturn(current);
        when(actionMapper.countActive("knowpost", 9L, "like")).thenReturn(7L);
        when(redis.opsForSet()).thenReturn(setOperations);
        when(redis.opsForZSet()).thenReturn(zSetOperations);

        processor.process(new ContentActionEventEntity(10L, 1L, "knowpost", 9L, "like"));

        verify(redis).execute(any(org.springframework.data.redis.core.RedisCallback.class));
        verify(setOperations).add(eq("bm:index:like:knowpost:9"), any(String.class));
        verify(zSetOperations).add(eq("ua:like:1"), eq("9"), anyDouble());
        verify(counterService).synchronizeCount("knowpost", "9", "like", 7L);
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void inactiveFactRemovesReverseIndexAndSynchronizesZero() {
        ContentActionEntity current = new ContentActionEntity();
        current.setId(10L);
        current.setActionStatus(0);
        when(actionMapper.findByKey(1L, "knowpost", 9L, "fav")).thenReturn(current);
        when(actionMapper.countActive("knowpost", 9L, "fav")).thenReturn(0L);
        when(redis.opsForSet()).thenReturn(setOperations);
        when(redis.opsForZSet()).thenReturn(zSetOperations);

        processor.process(new ContentActionEventEntity(10L, 1L, "knowpost", 9L, "fav"));

        verify(zSetOperations).remove("ua:fav:1", "9");
        verify(counterService).synchronizeCount("knowpost", "9", "fav", 0L);
    }
}
