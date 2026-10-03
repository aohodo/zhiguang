package com.chuanqing.vo;

import com.chuanqing.common.enums.VerificationSceneEnums;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送验证码结果。
 * <p>
 * 返回规范化账号、发送场景与验证码有效期（秒）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendCodeResultVO {

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
