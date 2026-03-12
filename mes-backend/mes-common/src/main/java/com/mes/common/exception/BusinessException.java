package com.mes.common.exception;

import com.mes.common.result.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常
 * <p>用于在 Service 层主动抛出可预知的业务错误</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 错误码 */
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.FAIL.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode, String detail) {
        super(resultCode.getMessage() + ": " + detail);
        this.code = resultCode.getCode();
    }
}
