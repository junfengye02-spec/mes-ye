package com.mes.dispatch.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 派工任务表实体
 */
@Data
@TableName("mes_dispatch_task")
public class DispatchTask implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long workOrderId;

    /** 工作清单ID */
    private Long workOrderTaskId;

    /** 订单编号 */
    private String orderNo;

    /** 工序号 */
    private String processNo;

    /** 工作名称 */
    private String workName;

    /** 计划工作中心 */
    private Long planWorkCenterId;

    /** 序列号 */
    private String serialNo;

    /** 项目 */
    private String projectName;

    /** 计划数量 */
    private BigDecimal planQty;

    /** 数量单位 */
    private String qtyUnit;

    /** 分派状态 */
    private String dispatchStatus;

    /** 计划开始时间 */
    private LocalDateTime planStartTime;

    /** 计划结束时间 */
    private LocalDateTime planEndTime;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 修改时间 */
    private LocalDateTime updatedTime;
}
