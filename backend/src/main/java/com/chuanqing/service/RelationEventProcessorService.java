package com.chuanqing.service;

import com.chuanqing.entity.RelationEventEntity;
import com.chuanqing.mapper.RelationMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.chuanqing.service.UserCounterService;
import org.springframework.stereotype.Service;
import java.time.Duration;

/**
 * 关系事件处理器。
 * 职责：幂等投影 FollowCreated/FollowCanceled 事件，更新粉丝表与关系缓存，并根据数据库事实重建用户计数。
 */
@Service
public class RelationEventProcessorService {
    private final RelationMapper mapper;
    private final StringRedisTemplate redis;
    private final UserCounterService userCounterService;

    public RelationEventProcessorService(RelationMapper mapper, StringRedisTemplate redis, UserCounterService userCounterService) {
        this.mapper = mapper;
        this.redis = redis;
        this.userCounterService = userCounterService;
    }

    /**
     * 处理关系事件：幂等落库、更新缓存，并从事实数据重建计数。
     * @param evt 关系事件
     */
    public void process(RelationEventEntity evt) {
        if (evt == null || evt.fromUserId() == null || evt.toUserId() == null) {
            throw new IllegalArgumentException("关系事件缺少必要字段");
        }
        if ("FollowCreated".equals(evt.type())) {
            if (evt.id() == null) {
                throw new IllegalArgumentException("关注创建事件缺少关系 ID");
            }
            // 异步插入粉丝表
            mapper.insertFollower(evt.id(), evt.toUserId(), evt.fromUserId(), 1);
            long now = System.currentTimeMillis();

            // 更新关注表与粉丝表缓存：ZSet 按时间分数维护最近项，设置短 TTL 减少陈旧数据
            redis.opsForZSet().add("uf:flws:" + evt.fromUserId(), String.valueOf(evt.toUserId()), now);
            redis.opsForZSet().add("uf:fans:" + evt.toUserId(), String.valueOf(evt.fromUserId()), now);
            redis.expire("uf:flws:" + evt.fromUserId(), Duration.ofHours(2));
            redis.expire("uf:fans:" + evt.toUserId(), Duration.ofHours(2));

        } else if ("FollowCanceled".equals(evt.type())) {
            mapper.cancelFollower(evt.toUserId(), evt.fromUserId());

            // 更新关注表与粉丝表缓存：移除 ZSet 项并刷新 TTL
            redis.opsForZSet().remove("uf:flws:" + evt.fromUserId(), String.valueOf(evt.toUserId()));
            redis.opsForZSet().remove("uf:fans:" + evt.toUserId(), String.valueOf(evt.fromUserId()));
            redis.expire("uf:flws:" + evt.fromUserId(), Duration.ofHours(2));
            redis.expire("uf:fans:" + evt.toUserId(), Duration.ofHours(2));

        } else {
            throw new IllegalArgumentException("不支持的关系事件类型: " + evt.type());
        }

        // 基于数据库事实重建，消息重放不会重复累加计数。
        userCounterService.rebuildAllCounters(evt.fromUserId());
        userCounterService.rebuildAllCounters(evt.toUserId());
    }
}
