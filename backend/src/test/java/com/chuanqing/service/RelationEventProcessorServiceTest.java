package com.chuanqing.service;

import com.chuanqing.entity.RelationEventEntity;
import com.chuanqing.mapper.RelationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelationEventProcessorServiceTest {

    @Mock
    private RelationMapper relationMapper;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private UserCounterService userCounterService;

    private RelationEventProcessorService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForZSet()).thenReturn(zSetOperations);
        service = new RelationEventProcessorService(relationMapper, redis, userCounterService);
    }

    @Test
    void replayedFollowEventRebuildsCountersFromFactsInsteadOfAddingDelta() {
        RelationEventEntity event = new RelationEventEntity("FollowCreated", 1L, 2L, 3L);

        service.process(event);
        service.process(event);

        verify(relationMapper, times(2)).insertFollower(3L, 2L, 1L, 1);
        verify(userCounterService, times(2)).rebuildAllCounters(1L);
        verify(userCounterService, times(2)).rebuildAllCounters(2L);
    }
}
