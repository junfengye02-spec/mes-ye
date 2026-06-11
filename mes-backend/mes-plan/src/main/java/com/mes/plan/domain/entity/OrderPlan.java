package com.mes.plan.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单计划主表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_order_plan")
public class OrderPlan extends BaseEntity {

    /** 订单号 */
    private String orderNo;

    /** 产品编码 */
    private String productCode;

    /** 产品名称 */
    private String productName;

    /** 项目 */
    private String projectName;

    /** WBS元素 */
    private String wbsElement;

    /** 新制维修类型 */
    private String newOrRepairType;

    /** 业务类型（维修/检查/主机） */
    private String businessType;

    /** 机型 */
    private String machineModel;

    /** 产品类别 */
    private String productCategory;

    /** 产品类型 */
    private String productType;

    /** 计划数量 */
    private BigDecimal planQty;

    /** 数量单位 */
    private String qtyUnit;

    /** 工厂组织 */
    private String factoryOrg;

    /** 计划组织 */
    private String planOrg;

    /** 主制组织 */
    private String mainOrg;

    /** 计划工作中心 */
    private Long planWorkCenterId;

    /** 状态（CREATED/RELEASED/COMPLETED/TERMINATED） */
    private String status;

    /** 流程状态（RUNNING/COMPLETED/TERMINATED） */
    private String flowStatus;

    /** 展开状态（UNEXPANDED/EXPANDED） */
    private String expandStatus;

    /** 完工状态（NOT_STARTED/APPROVED） */
    private String completionStatus;

    /** 是否订单 */
    private Boolean isOrder;

    /** 流程编码 */
    private String flowCode;

    /** 计划开始时间 */
    private LocalDateTime planStartTime;

    /** 计划结束时间 */
    private LocalDateTime planEndTime;

    /** 实际开始时间 */
    private LocalDateTime actualStartTime;

    /** 实际结束时间 */
    private LocalDateTime actualEndTime;

    /** 数据来源（MANUAL/APS） */
    private String dataSource;

    /** APS订单ID */
    private Long apsOrderId;

    /** APS同步批次号 */
    private String apsSyncBatchId;

    /** APS同步状态 */
    private String apsSyncStatus;
}
