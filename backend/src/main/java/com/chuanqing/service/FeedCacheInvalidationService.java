package com.chuanqing.service;

import com.chuanqing.entity.CounterEventEntity;
import com.github.benmanes.caffeine.cache.Cache;
import com.chuanqing.vo.FeedPageVO;
import com.chuanqing.entity.KnowPostEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Feed 页面缓存失效与作者计数重建监听器。
 *
 * <p>职责：</p>
 * - 监听点赞/收藏等计数事件（仅处理实体类型为 "knowpost"）；
 * - 根据“页面反向索引”（`feed:public:index:{eid}:{hour}`）定位受影响页面，
 *   删除本地 Caffeine 与 Redis 页面缓存；
 * - 根据内容计数事实重建创作者收到的点赞/收藏计数。
 *
 * <p>设计要点：</p>
 * - 不使用事件 delta 修改页面或作者计数，避免消息重放造成重复累加；
 * - 后续请求从精确计数投影重新构建 Feed 页面；
 * - 反向索引按小时维护，监听器会同时覆盖当前与上一个小时段的页面键。
 */
@Component
public class FeedCacheInvalidationService {

    private final Cache<String, FeedPageVO> feedPublicCache;
    private final StringRedisTemplate redis;
    private final UserCounterService userCounterService;
    private final com.chuanqing.mapper.KnowPostMapper knowPostMapper;

    public FeedCacheInvalidationService(@Qualifier("feedPublicCache") Cache<String, FeedPageVO> feedPublicCache,
                                         StringRedisTemplate redis,
                                         UserCounterService userCounterService,
                                         com.chuanqing.mapper.KnowPostMapper knowPostMapper) {
        this.feedPublicCache = feedPublicCache;
        this.redis = redis;
        this.userCounterService = userCounterService;
        this.knowPostMapper = knowPostMapper;
    }

    /**
     * 监听行为投影事件并失效相关缓存。
     *
     * <p>流程：</p>
     * - 仅处理实体类型为 "knowpost" 的 like/fav 事件；
     * - 若可解析到内容作者，则从精确事实重建其获赞/获藏计数；
     * - 通过最近两小时的反向索引集合定位并删除受影响页面；
     * - 同步清理页面索引引用。
     */
    @EventListener
    public void onCounterChanged(CounterEventEntity event) {
        if (!"knowpost".equals(event.getEntityType())) {
            return;
        }

        String metric = event.getMetric();
        if ("like".equals(metric) || "fav".equals(metric)) {
            String eid = event.getEntityId();
            KnowPostEntity post = knowPostMapper.findById(Long.valueOf(eid));
            if (post != null && post.getCreatorId() != null) {
                // 从数据库事实与精确内容计数重建，重复投影不会重复增减作者计数。
                userCounterService.rebuildAllCounters(post.getCreatorId());
            }

            long hourSlot = System.currentTimeMillis() / 3600000L;
            Set<String> keys = new LinkedHashSet<>();
            String currentIndex = "feed:public:index:" + eid + ":" + hourSlot;
            String previousIndex = "feed:public:index:" + eid + ":" + (hourSlot - 1);
            Set<String> cur = redis.opsForSet().members(currentIndex);
            if (cur != null) {
                keys.addAll(cur);
            }

            Set<String> prev = redis.opsForSet().members(previousIndex);
            if (prev != null) {
                keys.addAll(prev);
            }

            for (String key : keys) {
                feedPublicCache.invalidate(key);
                redis.delete(key);
                redis.opsForSet().remove(currentIndex, key);
                redis.opsForSet().remove(previousIndex, key);
            }
        }
    }
}
