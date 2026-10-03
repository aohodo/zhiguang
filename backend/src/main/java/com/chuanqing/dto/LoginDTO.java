package com.chuanqing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.chuanqing.common.enums.IdentifierTypeEnums;

/**
 * 登录请求。
 * <p>
 * 支持两种渠道：
 * - 验证码登录：填写 `code`；
 * - 密码登录：填写 `password`（用户已设置时）。
 * `identifierType` 指定账号类型（手机号/邮箱），`identifier` 为账号值。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {

    private @NotNull(message = "账号类型不能为空") IdentifierTypeEnums identifierType;
    private @NotBlank(message = "账号不能为空") String identifier;
    private String code;
    private String password;

    public IdentifierTypeEnums identifierType() {
        return identifierType;
    }

    public String identifier() {
        return identifier;
    }

    public String code() {
        return code;
    }

    public String password() {
        return password;
    }

}
