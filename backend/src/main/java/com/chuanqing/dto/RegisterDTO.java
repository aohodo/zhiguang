package com.chuanqing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.chuanqing.common.enums.IdentifierTypeEnums;

/**
 * 注册请求。
 * <p>
 * 字段：账号类型与值、验证码、可选密码、是否同意服务条款。
 * 验证：需通过验证码校验；当提供密码时需通过密码策略校验。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDTO {

    private @NotNull(message = "账号类型不能为空") IdentifierTypeEnums identifierType;
    private @NotBlank(message = "账号不能为空") String identifier;
    private @NotBlank(message = "验证码不能为空") String code;
    private String password;
    private boolean agreeTerms;

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

    public boolean agreeTerms() {
        return agreeTerms;
    }

}
