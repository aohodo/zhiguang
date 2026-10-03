package com.chuanqing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 预签名直传请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoragePresignDTO {

    @NotBlank
    private String scene; // knowpost_content | knowpost_image
    @NotBlank
    private String postId; // 字符串避免前端精度丢失
    @NotBlank
    private String contentType;
    private String ext;

    public String scene() {
        return scene;
    }

    public String postId() {
        return postId;
    }

    public String contentType() {
        return contentType;
    }

    public String ext() {
        return ext;
    }
}
