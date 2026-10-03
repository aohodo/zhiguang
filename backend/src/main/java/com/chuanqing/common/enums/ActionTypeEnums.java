package com.chuanqing.common.enums;

import com.chuanqing.utils.CounterSchemaUtils;

public enum ActionTypeEnums {
    LIKE("like", CounterSchemaUtils.IDX_LIKE),
    FAVORITE("fav", CounterSchemaUtils.IDX_FAV);

    private final String value;
    private final int counterIndex;

    ActionTypeEnums(String value, int counterIndex) {
        this.value = value;
        this.counterIndex = counterIndex;
    }

    public String getValue() {
        return value;
    }

    public int getCounterIndex() {
        return counterIndex;
    }

    public static ActionTypeEnums fromValue(String value) {
        for (ActionTypeEnums type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的行为类型: " + value);
    }
}
