package com.chuanqing.service;

import com.chuanqing.entity.KnowPostLifecycleEventEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowPostLifecycleEventService {

    private final UserCounterService userCounterService;
    private final RagIndexService ragIndexService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(KnowPostLifecycleEventEntity event) {
        if (event.getPostCountDelta() != 0) {
            try {
                userCounterService.incrementPosts(event.getCreatorId(), event.getPostCountDelta());
            } catch (Exception exception) {
                log.warn("知文计数投影更新失败，postId={}", event.getPostId(), exception);
            }
        }

        if (event.isIndexAfterCommit()) {
            try {
                ragIndexService.ensureIndexed(event.getPostId());
            } catch (Exception exception) {
                log.warn("知文发布后预索引失败，postId={}", event.getPostId(), exception);
            }
        }
    }
}
