package com.chuanqing.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContentActionEntity {
    private Long id;
    private Long userId;
    private String entityType;
    private Long entityId;
    private String actionType;
    private Integer actionStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
