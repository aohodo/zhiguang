package com.chuanqing.service;

import java.util.List;
import java.util.Map;
import com.chuanqing.vo.ActionItemVO;

public interface CounterService {
    /**
     * 点赞：仅在数据库状态从未点赞变为已点赞时生效。
     * @return 是否发生状态变化（true 表示这次操作生效）
     */
    boolean like(String entityType, String entityId, long userId);

    /**
     * 取消点赞：仅在数据库状态从已点赞变为未点赞时生效。
     * @return 是否发生状态变化（true 表示这次操作生效）
     */
    boolean unlike(String entityType, String entityId, long userId);

    /**
     * 收藏：仅在数据库状态发生变化时生效。
     */
    boolean fav(String entityType, String entityId, long userId);

    /**
     * 取消收藏：重复请求保持幂等。
     */
    boolean unfav(String entityType, String entityId, long userId);

    /**
     * 获取指定指标的计数。
     */
    Map<String, Long> getCounts(String entityType, String entityId, List<String> metrics);

    Map<String, Map<String, Long>> getCountsBatch(String entityType, List<String> entityIds, List<String> metrics);

    /**
     * 判断是否点赞/收藏（位图）。
     */
    boolean isLiked(String entityType, String entityId, long userId);
    boolean isFaved(String entityType, String entityId, long userId);

    /** 将指定指标同步为数据库事实计数。 */
    void synchronizeCount(String entityType, String entityId, String metric, long count);

    /** 查询当前用户的有效点赞或收藏行为。 */
    List<ActionItemVO> listMyActions(long userId, String actionType, int limit, int offset);
}
