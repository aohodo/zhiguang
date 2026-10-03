package com.chuanqing.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 令牌响应。
 * <p>
 * 返回访问令牌与刷新令牌及其过期时间，供客户端持久化与后续调用使用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenVO {

    private String accessToken;
    private Instant accessTokenExpiresAt;
    private String refreshToken;
    private Instant refreshTokenExpiresAt;

    public String accessToken() {
        return accessToken;
    }

    public Instant accessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public String refreshToken() {
        return refreshToken;
    }

    public Instant refreshTokenExpiresAt() {
        return refreshTokenExpiresAt;
    }

}
