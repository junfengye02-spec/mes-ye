package com.mes.abnormal.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 异常联络单状态枚举
 */
@Getter
@AllArgsConstructor
public enum AbnormalContactStatus {

    DRAFT("DRAFT", "草稿"),
    SUBMITTED("SUBMITTED", "已提交"),
    PROCESSING("PROCESSING", "处理中"),
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String desc;
}
