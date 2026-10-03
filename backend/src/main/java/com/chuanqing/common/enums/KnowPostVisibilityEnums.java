package com.chuanqing.common.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 知文可见范围。
 */
@Getter
public enum KnowPostVisibilityEnums {
    PUBLIC("public"),
    FOLLOWERS("followers"),
    SCHOOL("school"),
    PRIVATE("private"),
    UNLISTED("unlisted");

    private final String value;

    KnowPostVisibilityEnums(String value) {
        this.value = value;
    }

    public static KnowPostVisibilityEnums fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Know post visibility is required");
        }
        return Arrays.stream(values())
                .filter(visibility -> visibility.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported know post visibility: " + value));
    }
}
