package com.chuanqing.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.chuanqing.common.enums.VerificationSceneEnums;

/**
 * 发送验证码响应。
 * <p>
 * 返回规范化后的账号、场景，以及验证码有效期（秒）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendCodeVO {

    private String identifier;
    private VerificationSceneEnums scene;
    private int expireSeconds;

    public String identifier() {
        return identifier;
    }

    public VerificationSceneEnums scene() {
        return scene;
    }

    public int expireSeconds() {
        return expireSeconds;
    }

}
