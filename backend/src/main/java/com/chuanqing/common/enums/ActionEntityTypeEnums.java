package com.chuanqing.common.enums;

import java.util.Locale;

public enum ActionEntityTypeEnums {
    KNOW_POST("knowpost");

    private final String value;

    ActionEntityTypeEnums(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ActionEntityTypeEnums fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("实体类型不能为空");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ActionEntityTypeEnums type : values()) {
            if (type.value.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的实体类型: " + value);
    }
}
