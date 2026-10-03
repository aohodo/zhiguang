package com.chuanqing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新令牌请求。
 * <p>
 * 传入旧的刷新令牌，服务器验证后返回新的访问/刷新令牌对。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshDTO {

    private @NotBlank(message = "刷新令牌不能为空") String refreshToken;

    public String refreshToken() {
        return refreshToken;
    }

}
