package com.chuanqing.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应。
 * <p>
 * 登录/注册成功后返回：包含用户信息与令牌信息的组合结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthVO {

    private AuthUserVO user;
    private TokenVO token;

    public AuthUserVO user() {
        return user;
    }

    public TokenVO token() {
        return token;
    }

}
