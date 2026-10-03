package com.chuanqing.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentActionEventEntity {
    private Long actionId;
    private Long userId;
    private String entityType;
    private Long entityId;
    private String actionType;
}
