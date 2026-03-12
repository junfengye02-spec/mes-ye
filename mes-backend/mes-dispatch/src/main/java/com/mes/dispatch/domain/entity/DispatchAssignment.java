package com.mes.dispatch.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 派工分配表实体
 */
@Data
@TableName("mes_dispatch_assignment")
public class DispatchAssignment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 派工任务ID */
    private Long dispatchTaskId;

    /** 分派类型（PERSON/DEVICE/TEAM） */
    private String assignType;

    /** 分派对象ID */
    private Long assigneeId;

    /** 分派对象编码 */
    private String assigneeCode;

    /** 分派对象名称 */
    private String assigneeName;

    /** 分派数量 */
    private BigDecimal assignedQty;

    /** 数量单位 */
    private String qtyUnit;

    /** 分派状态（ACTIVE/REVOKED） */
    private String status;

    /** 派工人 */
    private String assignedBy;

    /** 派工时间 */
    private LocalDateTime assignedTime;

    /** 撤销人 */
    private String revokedBy;

    /** 撤销时间 */
    private LocalDateTime revokedTime;
}
