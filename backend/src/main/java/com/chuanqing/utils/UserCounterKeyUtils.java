package com.chuanqing.utils;

/**
 * 用户维度计数键生成工具。
 */
public final class UserCounterKeyUtils {
    private UserCounterKeyUtils() {}

    public static String sdsKey(long userId) {
        return "ucnt:" + userId; // 用户维度固定结构计数（SDS）键
    }
}

