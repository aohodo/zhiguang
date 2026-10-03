package com.chuanqing.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 构造关系事件。
 *
 * @param type       事件类型
 * @param fromUserId 触发方用户ID
 * @param toUserId   目标方用户ID
 * @param id         关系记录ID，可为空
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelationEventEntity {

    private String type;
    private Long fromUserId;
    private Long toUserId;
    private Long id;

    public String type() {
        return type;
    }

    public Long fromUserId() {
        return fromUserId;
    }

    public Long toUserId() {
        return toUserId;
    }

    public Long id() {
        return id;
    }

}
