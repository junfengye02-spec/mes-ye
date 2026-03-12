package com.mes.abnormal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 异常联络单状态日志表实体
 */
@Data
@TableName("mes_abnormal_contact_log")
public class AbnormalContactLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 异常联络单ID */
    private Long contactId;

    /** 原状态 */
    private String fromStatus;

    /** 新状态 */
    private String toStatus;

    /** 动作 */
    private String action;

    /** 操作人 */
    private String operator;

    /** 操作时间 */
    private LocalDateTime operatedTime;

    /** 说明 */
    private String remark;
}
