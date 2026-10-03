package com.chuanqing.common.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 知文生命周期状态。
 */
@Getter
public enum KnowPostStatusEnums {
    DRAFT("draft"),
    PENDING_REVIEW("pending_review"),
    PUBLISHED("published"),
    REJECTED("rejected"),
    OFFLINE("offline"),
    DELETED("deleted");

    private final String value;

    KnowPostStatusEnums(String value) {
        this.value = value;
    }

    public static KnowPostStatusEnums fromValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported know post status: " + value));
    }
}
