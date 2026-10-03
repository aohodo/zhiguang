package com.chuanqing.service;

import com.chuanqing.entity.KnowPostLifecycleEventEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KnowPostLifecycleEventServiceTest {

    @Mock
    private UserCounterService userCounterService;

    @Mock
    private RagIndexService ragIndexService;

    @InjectMocks
    private KnowPostLifecycleEventService service;

    @Test
    void publishedEventUpdatesCounterAndWarmsRagIndex() {
        service.handle(new KnowPostLifecycleEventEntity(101L, 7L, 1, true));

        verify(userCounterService).incrementPosts(7L, 1);
        verify(ragIndexService).ensureIndexed(101L);
    }

    @Test
    void deletedEventOnlyDecrementsCounter() {
        service.handle(new KnowPostLifecycleEventEntity(102L, 7L, -1, false));

        verify(userCounterService).incrementPosts(7L, -1);
        verify(ragIndexService, never()).ensureIndexed(102L);
    }
}
