package com.mes.workorder.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工作清单表实体
 */
@Data
@TableName("mes_work_order_task")
public class WorkOrderTask implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long workOrderId;

    /** 工序号/工作编号 */
    private String taskNo;

    /** 工作名称 */
    private String taskName;

    /** 计划工作中心 */
    private Long planWorkCenterId;

    /** 计划数量 */
    private BigDecimal planQty;

    /** 数量单位 */
    private String qtyUnit;

    /** 状态 */
    private String status;

    /** 顺序号 */
    private Integer sequenceNo;

    /** 序列号 */
    private String serialNo;

    /** 项目 */
    private String projectName;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 修改时间 */
    private LocalDateTime updatedTime;
}
