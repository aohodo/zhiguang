package com.chuanqing.service.impl;

import com.chuanqing.common.enums.ActionEntityTypeEnums;
import com.chuanqing.common.enums.ActionTypeEnums;
import com.chuanqing.entity.BitmapShardEntity;
import com.chuanqing.entity.ContentActionEntity;
import com.chuanqing.exception.BusinessException;
import com.chuanqing.common.enums.ErrorCodeEnums;
import com.chuanqing.mapper.ContentActionMapper;
import com.chuanqing.mapper.OutboxMapper;
import com.chuanqing.service.CounterService;
import com.chuanqing.service.KnowPostPermissionService;
import com.chuanqing.utils.CounterKeyUtils;
import com.chuanqing.utils.CounterSchemaUtils;
import com.chuanqing.utils.SnowflakeIdGeneratorUtils;
import com.chuanqing.vo.ActionItemVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.redisson.api.RedissonClient;
import org.redisson.api.RLock;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RBucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 内容行为与计数服务实现（MySQL 事实 + Redis 位图/SDS 投影）。
 *
 * <p>职责：</p>
 * - MySQL 行为事实与 Outbox 同事务写入；
 * - 读取汇总计数（SDS），异常时基于 MySQL 行为事实重建；
 * - 批量读取优化与“是否点赞/收藏”判定。
 */
@Slf4j
@Service
public class CounterServiceImpl implements CounterService {

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> setCounterScript;
    private final RedissonClient redisson;
    private final ContentActionMapper actionMapper;
    private final KnowPostPermissionService permissionService;
    private final OutboxMapper outboxMapper;
    private final SnowflakeIdGeneratorUtils idGenerator;
    private final ObjectMapper objectMapper;
    @Value("${counter.rebuild.lock.ttl-ms:5000}")
    private long lockTtlMs;
    @Value("${counter.rebuild.rate.permits:3}")
    private int ratePermits;
    @Value("${counter.rebuild.rate.window-seconds:10}")
    private int rateWindowSeconds;
    @Value("${counter.rebuild.backoff.base-ms:500}")
    private long backoffBaseMs;
    @Value("${counter.rebuild.backoff.max-ms:30000}")
    private long backoffMaxMs;

    public CounterServiceImpl(StringRedisTemplate redis,
                              RedissonClient redisson,
                              ContentActionMapper actionMapper,
                              KnowPostPermissionService permissionService,
                              OutboxMapper outboxMapper,
                              SnowflakeIdGeneratorUtils idGenerator,
                              ObjectMapper objectMapper) {
        this.redis = redis;
        this.redisson = redisson;
        this.actionMapper = actionMapper;
        this.permissionService = permissionService;
        this.outboxMapper = outboxMapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
        this.setCounterScript = new DefaultRedisScript<>();
        this.setCounterScript.setResultType(Long.class);
        this.setCounterScript.setScriptText(SET_COUNTER_FIELD_LUA);
    }

    /**
     * 点赞：仅当数据库事实从未点赞变为已点赞时返回 true 并写 Outbox。
     * @param entityType 实体类型
     * @param entityId 实体 ID
     * @param userId 用户 ID
     * @return 是否发生状态变化（幂等）
     */
    @Override
    @Transactional
    public boolean like(String entityType, String entityId, long userId) {
        return changeAction(entityType, entityId, userId, ActionTypeEnums.LIKE, true);
    }

    /**
     * 取消点赞：仅当数据库事实从已点赞变为未点赞时返回 true。
     */
    @Override
    @Transactional
    public boolean unlike(String entityType, String entityId, long userId) {
        return changeAction(entityType, entityId, userId, ActionTypeEnums.LIKE, false);
    }

    /**
     * 收藏：仅在数据库事实发生状态变化时写 Outbox。
     */
    @Override
    @Transactional
    public boolean fav(String entityType, String entityId, long userId) {
        return changeAction(entityType, entityId, userId, ActionTypeEnums.FAVORITE, true);
    }

    /**
     * 取消收藏：重复调用保持幂等。
     */
    @Override
    @Transactional
    public boolean unfav(String entityType, String entityId, long userId) {
        return changeAction(entityType, entityId, userId, ActionTypeEnums.FAVORITE, false);
    }

    /** 数据库状态切换与 Outbox 写入。 */
    private boolean changeAction(String entityType,
                                 String entityId,
                                 long userId,
                                 ActionTypeEnums actionType,
                                 boolean active) {
        ActionEntityTypeEnums targetType = requireSupportedEntityType(entityType);
        long targetId = requireEntityId(entityId);
        permissionService.requireReadable(targetId, userId);

        int changed = active
                ? actionMapper.activateExisting(userId, targetType.getValue(), targetId, actionType.getValue())
                : actionMapper.deactivate(userId, targetType.getValue(), targetId, actionType.getValue());

        Long actionId = null;
        if (active && changed == 0) {
            long candidateId = idGenerator.nextId();
            changed = actionMapper.insertIfAbsent(
                    candidateId, userId, targetType.getValue(), targetId, actionType.getValue());
            if (changed == 1) {
                actionId = candidateId;
            }
        }

        if (changed == 0) {
            return false;
        }
        if (actionId == null) {
            ContentActionEntity action = actionMapper.findByKey(
                    userId, targetType.getValue(), targetId, actionType.getValue());
            if (action == null || action.getId() == null) {
                throw new IllegalStateException("行为状态已变化但记录不存在");
            }
            actionId = action.getId();
        }

        writeActionOutbox(actionId, userId, targetType, targetId, actionType, active);
        return true;
    }

    private ActionEntityTypeEnums requireSupportedEntityType(String entityType) {
        try {
            return ActionEntityTypeEnums.fromValue(entityType);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCodeEnums.BAD_REQUEST, exception.getMessage());
        }
    }

    private long requireEntityId(String entityId) {
        try {
            long value = Long.parseLong(entityId);
            if (value <= 0) {
                throw new NumberFormatException("non-positive");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCodeEnums.BAD_REQUEST, "实体 ID 非法");
        }
    }

    private void writeActionOutbox(Long actionId,
                                   long userId,
                                   ActionEntityTypeEnums entityType,
                                   long entityId,
                                   ActionTypeEnums actionType,
                                   boolean active) {
        long eventId = idGenerator.nextId();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", eventId);
        payload.put("eventType", active ? "ContentActionActivated" : "ContentActionDeactivated");
        payload.put("eventVersion", 1);
        payload.put("occurredAt", java.time.Instant.now().toString());
        payload.put("entity", "content_action");
        payload.put("actionId", actionId);
        payload.put("userId", userId);
        payload.put("entityType", entityType.getValue());
        payload.put("entityId", entityId);
        payload.put("actionType", actionType.getValue());

        try {
            String json = objectMapper.writeValueAsString(payload);
            int inserted = outboxMapper.insert(eventId, "content_action", actionId,
                    active ? "ContentActionActivated" : "ContentActionDeactivated", json);
            if (inserted != 1) {
                throw new IllegalStateException("行为事件写入失败");
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("行为事件序列化失败", exception);
        }
    }

    /**
     * 获取实体计数汇总（SDS）。
     * 若缺失或结构异常则触发基于 MySQL 行为事实的重建。
     */
    @Override
    public Map<String, Long> getCounts(String entityType, String entityId, List<String> metrics) {
        String sdsKey = CounterKeyUtils.sdsKey(entityType, entityId);
        int expectedLen = CounterSchemaUtils.SCHEMA_LEN * CounterSchemaUtils.FIELD_SIZE;
        // SDS 固定结构：按大端 32 位编码
        byte[] raw = getRaw(sdsKey);
        boolean needRebuild = (raw == null || raw.length != expectedLen);

        Map<String, Long> result = new LinkedHashMap<>();

        if (needRebuild) {
            log.info("计数结构不存在，需要重建");
            // 限流与指数退避：避免在热点实体上触发重建风暴
            if (inBackoff(entityType, entityId)) {
                for (String m : metrics) {
                    result.put(m, 0L);
                }
                return result;
            }

            if (!allowedByRateLimiter(entityType, entityId)) {
                escalateBackoff(entityType, entityId);
                for (String m : metrics) {
                    result.put(m, 0L);
                }
                return result;
            }

            String lockKey = String.format("lock:sds-rebuild:%s:%s", entityType, entityId);

            RLock lock = redisson.getLock(lockKey);
            boolean locked = false;

            try {
                // 使用 Redisson 看门狗机制：不指定租期，自动续约（由 Redisson 的 lockWatchdogTimeout 控制）
                locked = lock.tryLock(0L, TimeUnit.MILLISECONDS);
                if (!locked) {
                    escalateBackoff(entityType, entityId);
                    for (String m : metrics) {
                        result.put(m, 0L);
                    }
                    return result;
                }
                // 依据 MySQL 行为事实重建，Redis 全量丢失后仍可恢复。
                byte[] newSds = new byte[expectedLen];
                List<String> rebuildFields = new ArrayList<>();
                long targetId = requireEntityId(entityId);
                for (String m : metrics) {
                    Integer idx = CounterSchemaUtils.NAME_TO_IDX.get(m);
                    if (idx == null) {
                        continue;
                    }
                    long sum = actionMapper.countActive(entityType, targetId, m);
                    writeInt32BE(newSds, idx * CounterSchemaUtils.FIELD_SIZE, sum);
                    result.put(m, sum);
                    rebuildFields.add(String.valueOf(idx));
                }
                // 回写SDS并清理聚合桶，避免重复加算
                setRaw(sdsKey, newSds);
                if (!rebuildFields.isEmpty()) {
                    String aggKey = CounterKeyUtils.aggKey(entityType, entityId);
                    redis.opsForHash().delete(aggKey, rebuildFields.toArray());
                }
                resetBackoff(entityType, entityId);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                escalateBackoff(entityType, entityId);
                for (String m : metrics) {
                    result.put(m, 0L);
                }
                return result;
            } finally {
                if (locked) {
                    try {
                        lock.unlock();
                    } catch (Exception ignore) {}
                }
            }
        } else {
            for (String m : metrics) {
                Integer idx = CounterSchemaUtils.NAME_TO_IDX.get(m);
                if (idx == null) {
                    continue;
                }

                int off = idx * CounterSchemaUtils.FIELD_SIZE;
                long val = readInt32BE(raw, off); // 大端读取单段 32 位值
                result.put(m, val);
            }
        }
        return result;
    }

    /**
     * 批量获取实体计数（管道批量 GET 降低 RTT）。
     * 缺失或结构异常（长度不符）时按零返回，保证接口稳定。
     * @param entityType 实体类型
     * @param entityIds 实体ID列表
     * @param metrics 指标名列表
     * @return 每个实体的指标计数映射
     */
    @Override
    public Map<String, Map<String, Long>> getCountsBatch(String entityType, List<String> entityIds, List<String> metrics) {
        Map<String, Map<String, Long>> out = new LinkedHashMap<>();
        if (entityIds == null || entityIds.isEmpty() || metrics == null || metrics.isEmpty()) {
            return out;
        }

        List<String> keys = new ArrayList<>(entityIds.size());
        for (String eid : entityIds) {
            keys.add(CounterKeyUtils.sdsKey(entityType, eid));
        }

        // 管道批量 GET：将多个 SDS 读取合并到一次往返
        List<Object> raws = redis.executePipelined((RedisCallback<Object>) connection -> {
            for (String k : keys) {
                connection.stringCommands().get(k.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });

        int expectedLen = CounterSchemaUtils.SCHEMA_LEN * CounterSchemaUtils.FIELD_SIZE;
        for (int i = 0; i < entityIds.size(); i++) {
            String eid = entityIds.get(i);
            Object rawObj = i < raws.size() ? raws.get(i) : null;
            byte[] raw = (rawObj instanceof byte[]) ? (byte[]) rawObj : null;

            Map<String, Long> m = new LinkedHashMap<>();
            if (raw != null && raw.length == expectedLen) {
                for (String name : metrics) {
                    Integer idx = CounterSchemaUtils.NAME_TO_IDX.get(name);
                    if (idx == null) continue;
                    int off = idx * CounterSchemaUtils.FIELD_SIZE;
                    long val = readInt32BE(raw, off);
                    m.put(name, val);
                }
            } else {
                for (String name : metrics) {
                    m.put(name, 0L); // 缺失或异常结构时补零，避免接口失败与重建风暴
                }
            }
            out.put(eid, m);
        }
        return out;
    }

    /**
     * 是否点赞判定：基于分片位图在分片内做位测试。
     * 毫秒级读取，不依赖计数快照。
     */
    @Override
    public boolean isLiked(String entityType, String entityId, long userId) {
        long chunk = BitmapShardEntity.chunkOf(userId);
        long bit = BitmapShardEntity.bitOf(userId);
        return getBit(CounterKeyUtils.bitmapKey("like", entityType, entityId, chunk), bit);
    }

    /**
     * 是否收藏判定：同点赞，基于分片位图位测试。
     */
    @Override
    public boolean isFaved(String entityType, String entityId, long userId) {
        long chunk = BitmapShardEntity.chunkOf(userId);
        long bit = BitmapShardEntity.bitOf(userId);
        return getBit(CounterKeyUtils.bitmapKey("fav", entityType, entityId, chunk), bit);
    }

    @Override
    public void synchronizeCount(String entityType, String entityId, String metric, long count) {
        Integer index = CounterSchemaUtils.NAME_TO_IDX.get(metric);
        if (index == null) {
            throw new IllegalArgumentException("不支持的计数指标: " + metric);
        }
        redis.execute(
                setCounterScript,
                List.of(CounterKeyUtils.sdsKey(entityType, entityId)),
                String.valueOf(CounterSchemaUtils.SCHEMA_LEN),
                String.valueOf(CounterSchemaUtils.FIELD_SIZE),
                String.valueOf(index),
                String.valueOf(Math.max(0L, count))
        );
    }

    @Override
    public List<ActionItemVO> listMyActions(long userId, String actionType, int limit, int offset) {
        ActionTypeEnums type;
        try {
            type = ActionTypeEnums.fromValue(actionType);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCodeEnums.BAD_REQUEST, exception.getMessage());
        }
        return actionMapper.listActiveByUser(userId, type.getValue(), limit, offset).stream()
                .map(action -> new ActionItemVO(
                        action.getEntityType(), action.getEntityId(), action.getUpdatedAt()))
                .toList();
    }

    /**
     * 读取位图某偏移位（GETBIT）。
     * @param key 位图分片键
     * @param offset 分片内位偏移
     * @return 位是否为 1
     */
    private boolean getBit(String key, long offset) {
        Boolean bit = redis.execute((RedisCallback<Boolean>) connection ->
                connection.stringCommands().getBit(key.getBytes(StandardCharsets.UTF_8), offset));
        return Boolean.TRUE.equals(bit);
    }

    /**
     * 读取 SDS 原始字节（固定结构，长度=字段数×4）。
     */
    private byte[] getRaw(String key) {
        return redis.execute((RedisCallback<byte[]>) connection ->
                connection.stringCommands().get(key.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * 写入 SDS 原始字节（覆盖式写）。
     */
    private void setRaw(String key, byte[] val) {
        redis.execute((RedisCallback<Void>) connection -> {
            connection.stringCommands().set(key.getBytes(StandardCharsets.UTF_8), val);
            return null;
        });
    }

    /**
     * 是否处于指数退避期：期间跳过重建并返回降级结果。
     */
    private boolean inBackoff(String entityType, String entityId) {
        String bKey = String.format("backoff:sds-rebuild:until:%s:%s", entityType, entityId);
        RBucket<Long> bucket = redisson.getBucket(bKey);
        Long until = bucket.get();

        return until != null && System.currentTimeMillis() < until;
    }

    /**
     * 增加退避级别并设置下次允许尝试的时间（指数递增，封顶）。
     */
    private void escalateBackoff(String entityType, String entityId) {
        String eKey = String.format("backoff:sds-rebuild:exp:%s:%s", entityType, entityId);
        String uKey = String.format("backoff:sds-rebuild:until:%s:%s", entityType, entityId);

        RBucket<Integer> expB = redisson.getBucket(eKey);
        RBucket<Long> untilB = redisson.getBucket(uKey);
        Integer exp = expB.get();

        int nextExp = Math.min(exp == null ? 0 : exp + 1, 10);
        long delay = Math.min(backoffBaseMs * (1L << nextExp), backoffMaxMs);
        long until = System.currentTimeMillis() + delay;

        // 设置过期时间，避免长时间残留
        expB.set(nextExp);
        untilB.set(until, Duration.ofMillis(delay + 1000));
    }

    /**
     * 重置退避状态（成功重建后）。
     */
    private void resetBackoff(String entityType, String entityId) {
        String eKey = String.format("backoff:sds-rebuild:exp:%s:%s", entityType, entityId);
        String uKey = String.format("backoff:sds-rebuild:until:%s:%s", entityType, entityId);

        try {
            redisson.getBucket(eKey).delete();
        } catch (Exception ignore) {}

        try {
            redisson.getBucket(uKey).delete();
        } catch (Exception ignore) {}
    }

    /**
     * 限流判断：单位窗口可重建次数，防止抖动与风暴。
     */
    private boolean allowedByRateLimiter(String entityType, String entityId) {
        String rlKey = String.format("rl:sds-rebuild:%s:%s", entityType, entityId);
        RRateLimiter limiter = redisson.getRateLimiter(rlKey);

        // 初始化速率（如已存在则忽略）
        limiter.trySetRate(RateType.OVERALL, ratePermits, Duration.ofSeconds(rateWindowSeconds));

        return limiter.tryAcquire(1);
    }

    /**
     * 以大端序读取 32 位无符号整型。
     */
    private static long readInt32BE(byte[] buf, int off) {
        long n = 0;
        for (int i = 0; i < 4; i++) {
            n = (n << 8) | (buf[off + i] & 0xFFL);
        }
        return n;
    }

    /**
     * 以大端序写入 32 位无符号整型（截断到 0~2^32-1）。
     */
    private static void writeInt32BE(byte[] buf, int off, long val) {
        long n = Math.max(0, Math.min(val, 0xFFFF_FFFFL));
        buf[off] = (byte) ((n >>> 24) & 0xFF);
        buf[off + 1] = (byte) ((n >>> 16) & 0xFF);
        buf[off + 2] = (byte) ((n >>> 8) & 0xFF);
        buf[off + 3] = (byte) (n & 0xFF);
    }

    private static final String SET_COUNTER_FIELD_LUA = """
            local key = KEYS[1]
            local schemaLen = tonumber(ARGV[1])
            local fieldSize = tonumber(ARGV[2])
            local idx = tonumber(ARGV[3])
            local value = tonumber(ARGV[4])
            local function write32be(n)
              if n < 0 then n = 0 end
              if n > 4294967295 then n = 4294967295 end
              local t = {}
              for i=4,1,-1 do t[i] = n % 256; n = math.floor(n/256) end
              return string.char(unpack(t))
            end
            local current = redis.call('GET', key)
            if not current or string.len(current) ~= schemaLen * fieldSize then
              current = string.rep(string.char(0), schemaLen * fieldSize)
            end
            local off = idx * fieldSize
            local segment = write32be(value)
            current = string.sub(current, 1, off) .. segment .. string.sub(current, off + fieldSize + 1)
            redis.call('SET', key, current)
            return 1
            """;
}
