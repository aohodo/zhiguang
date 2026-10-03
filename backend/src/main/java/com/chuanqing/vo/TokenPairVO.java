package com.chuanqing.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 访问令牌与刷新令牌的组合。
 * <p>
 * 字段说明：
 * - accessToken：访问令牌（JWT 字符串，Bearer 使用）；
 * - accessTokenExpiresAt：访问令牌过期时间；
 * - refreshToken：刷新令牌（JWT 字符串，仅用于刷新接口）；
 * - refreshTokenExpiresAt：刷新令牌过期时间；
 * - refreshTokenId：刷新令牌 ID（jti，用于白名单存储与撤销）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenPairVO {

    private String accessToken;
    private Instant accessTokenExpiresAt;
    private String refreshToken;
    private Instant refreshTokenExpiresAt;
    private String refreshTokenId;

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

    public String refreshTokenId() {
        return refreshTokenId;
    }

}
