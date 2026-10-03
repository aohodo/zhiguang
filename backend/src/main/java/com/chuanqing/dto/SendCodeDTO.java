package com.chuanqing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.chuanqing.common.enums.IdentifierTypeEnums;
import com.chuanqing.common.enums.VerificationSceneEnums;

/**
 * 发送验证码请求。
 * <p>
 * `scene` 指定场景（注册/登录/重置密码），配合账号类型与值用于生成并发送验证码。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendCodeDTO {

    private @NotNull(message = "场景不能为空") VerificationSceneEnums scene;
    private @NotNull(message = "账号类型不能为空") IdentifierTypeEnums identifierType;
    private @NotBlank(message = "账号不能为空") String identifier;

    public VerificationSceneEnums scene() {
        return scene;
    }

    public IdentifierTypeEnums identifierType() {
        return identifierType;
    }

    public String identifier() {
        return identifier;
    }

}
