package com.chuanqing.service;

import com.chuanqing.common.enums.ActionEntityTypeEnums;
import com.chuanqing.common.enums.ActionTypeEnums;
import com.chuanqing.entity.BitmapShardEntity;
import com.chuanqing.entity.ContentActionEntity;
import com.chuanqing.entity.ContentActionEventEntity;
import com.chuanqing.entity.CounterEventEntity;
import com.chuanqing.mapper.ContentActionMapper;
import com.chuanqing.utils.CounterKeyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class ContentActionEventProcessorService {

    private final ContentActionMapper actionMapper;
    private final CounterService counterService;
    private final StringRedisTemplate redis;
    private final ApplicationEventPublisher eventPublisher;

    public void process(ContentActionEventEntity event) {
        validate(event);
        ActionEntityTypeEnums entityType = ActionEntityTypeEnums.fromValue(event.getEntityType());
        ActionTypeEnums actionType = ActionTypeEnums.fromValue(event.getActionType());
        ContentActionEntity current = actionMapper.findByKey(
                event.getUserId(), entityType.getValue(), event.getEntityId(), actionType.getValue());
        if (current == null) {
            throw new IllegalStateException("行为事实记录不存在");
        }

        boolean active = Integer.valueOf(1).equals(current.getActionStatus());
        long chunk = BitmapShardEntity.chunkOf(event.getUserId());
        long bit = BitmapShardEntity.bitOf(event.getUserId());
        String bitmapKey = CounterKeyUtils.bitmapKey(
                actionType.getValue(), entityType.getValue(), String.valueOf(event.getEntityId()), chunk);
        redis.execute((RedisCallback<Boolean>) connection -> connection.stringCommands().setBit(
                bitmapKey.getBytes(StandardCharsets.UTF_8), bit, active));
        redis.opsForSet().add(CounterKeyUtils.bitmapIndexKey(
                actionType.getValue(), entityType.getValue(), String.valueOf(event.getEntityId())), bitmapKey);

        String userActionKey = CounterKeyUtils.userActionKey(actionType.getValue(), event.getUserId());
        if (active) {
            double score = current.getUpdatedAt() == null
                    ? System.currentTimeMillis()
                    : current.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            redis.opsForZSet().add(userActionKey, String.valueOf(event.getEntityId()), score);
        } else {
            redis.opsForZSet().remove(userActionKey, String.valueOf(event.getEntityId()));
        }

        long count = actionMapper.countActive(
                entityType.getValue(), event.getEntityId(), actionType.getValue());
        counterService.synchronizeCount(
                entityType.getValue(), String.valueOf(event.getEntityId()), actionType.getValue(), count);

        eventPublisher.publishEvent(CounterEventEntity.of(
                entityType.getValue(), String.valueOf(event.getEntityId()),
                actionType.getValue(), actionType.getCounterIndex(), event.getUserId(), active ? 1 : -1));
    }

    private void validate(ContentActionEventEntity event) {
        if (event == null || event.getUserId() == null || event.getEntityId() == null
                || event.getEntityType() == null || event.getActionType() == null) {
            throw new IllegalArgumentException("行为事件缺少必要字段");
        }
    }
}
