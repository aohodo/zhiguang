package com.chuanqing.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 预签名直传响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoragePresignVO {

    private String objectKey;
    private String putUrl;
    private Map<String, String> headers;
    private int expiresIn;

    public String objectKey() {
        return objectKey;
    }

    public String putUrl() {
        return putUrl;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public int expiresIn() {
        return expiresIn;
    }
}