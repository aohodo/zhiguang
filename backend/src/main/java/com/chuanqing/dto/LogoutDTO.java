package com.chuanqing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 登出请求。
 * <p>
 * 传入刷新令牌以撤销对应会话，确保令牌不可再用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogoutDTO {

    private @NotBlank(message = "刷新令牌不能为空") String refreshToken;

    public String refreshToken() {
        return refreshToken;
    }

}
