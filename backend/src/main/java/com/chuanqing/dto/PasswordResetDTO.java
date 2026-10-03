package com.chuanqing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.chuanqing.common.enums.IdentifierTypeEnums;

/**
 * 重置密码请求。
 * <p>
 * 通过验证码校验身份后设置新密码。密码需满足复杂度策略。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetDTO {

    private @NotNull(message = "账号类型不能为空") IdentifierTypeEnums identifierType;
    private @NotBlank(message = "账号不能为空") String identifier;
    private @NotBlank(message = "验证码不能为空") String code;
    private @NotBlank(message = "新密码不能为空") String newPassword;

    public IdentifierTypeEnums identifierType() {
        return identifierType;
    }

    public String identifier() {
        return identifier;
    }

    public String code() {
        return code;
    }

    public String newPassword() {
        return newPassword;
    }

}
