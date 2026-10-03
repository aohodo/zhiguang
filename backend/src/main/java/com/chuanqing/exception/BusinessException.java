package com.chuanqing.exception;

import com.chuanqing.common.enums.ErrorCodeEnums;
import lombok.Getter;

/**
 * 业务异常。
 *
 * <p>用于在业务校验失败时携带明确的 {@link ErrorCodeEnums} 抛出，由全局异常处理器统一转为 HTTP 响应。</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 业务错误码，用于前端/调用方做稳定的错误分支处理。
     */
    private final ErrorCodeEnums errorCode;

    /**
     * 使用错误码的默认文案构造异常。
     *
     * @param errorCode 错误码（必填）
     */
    public BusinessException(ErrorCodeEnums errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /**
     * 使用自定义文案构造异常（错误码不变）。
     *
     * @param errorCode 错误码（必填）
     * @param message 自定义提示文案
     */
    public BusinessException(ErrorCodeEnums errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

}
