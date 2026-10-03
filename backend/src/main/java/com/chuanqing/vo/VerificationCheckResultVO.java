package com.chuanqing.vo;

import com.chuanqing.common.enums.VerificationCodeStatusEnums;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码校验结果。
 * <p>
 * 包含状态（成功/未找到/过期/错误/尝试过多）和次数统计信息，提供便捷成功判断。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCheckResultVO {

    private VerificationCodeStatusEnums status;
    private int attempts;
    private int maxAttempts;

    public VerificationCodeStatusEnums status() {
        return status;
    }

    public int attempts() {
        return attempts;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public boolean isSuccess() {
        return status == VerificationCodeStatusEnums.SUCCESS;
    }
}
