package com.mes.plan.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 计划状态日志表实体
 */
@Data
@TableName("mes_plan_status_log")
public class PlanStatusLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计划类型（ORDER/PRODUCTION） */
    private String planType;

    /** 计划ID */
    private Long planId;

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
