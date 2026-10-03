package com.chuanqing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 内容上传确认请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowPostContentConfirmDTO {

    private @NotBlank String objectKey;
    private @NotBlank String etag;
    private @NotNull Long size;
    private @NotBlank String sha256;

    public String objectKey() {
        return objectKey;
    }

    public String etag() {
        return etag;
    }

    public Long size() {
        return size;
    }

    public String sha256() {
        return sha256;
    }
}