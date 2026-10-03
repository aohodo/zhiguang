package com.chuanqing.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KnowPostLifecycleEventEntity {

    private final long postId;
    private final long creatorId;
    private final int postCountDelta;
    private final boolean indexAfterCommit;
}
