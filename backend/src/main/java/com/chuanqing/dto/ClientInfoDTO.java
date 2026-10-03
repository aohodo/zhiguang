package com.chuanqing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户端信息。
 * <p>
 * 记录客户端 IP 与 UserEntity-Agent，用于登录审计、风控与活动记录。
 * 该对象通常由控制器从 HTTP 请求中解析生成。
 *
 * @param ip        客户端 IP 地址（可能来自 `X-Forwarded-For` 或远端地址）。
 * @param userAgent 客户端 UserEntity-Agent 字符串。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientInfoDTO {

    private String ip;
    private String userAgent;

    public String ip() {
        return ip;
    }

    public String userAgent() {
        return userAgent;
    }

}
