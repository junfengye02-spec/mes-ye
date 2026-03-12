package com.mes.common.utils;

import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;

/**
 * 业务断言工具
 * <p>在 Service 层替代 if-throw 的简洁写法</p>
 */
public final class AssertUtil {

    private AssertUtil() {}

    /**
     * 断言不为空，否则抛出异常
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new BusinessException(message);
        }
    }

    /**
     * 断言不为空，否则抛出 DATA_NOT_EXIST
     */
    public static void notNull(Object object, ResultCode resultCode) {
        if (object == null) {
            throw new BusinessException(resultCode);
        }
    }

    /**
     * 断言条件为真，否则抛出异常
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message);
        }
    }

    /**
     * 断言条件为假，否则抛出异常
     */
    public static void isFalse(boolean condition, String message) {
        if (condition) {
            throw new BusinessException(message);
        }
    }
}
