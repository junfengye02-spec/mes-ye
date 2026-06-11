package com.mes.plan.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产计划主表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_production_plan")
public class ProductionPlan extends BaseEntity {

    /** 订单计划ID */
    private Long orderPlanId;

    /** 订单编号 */
    private String orderNo;

    /** 产品编码 */
    private String productCode;

    /** 产品名称 */
    private String productName;

    /** 新制维修类型 */
    private String newOrRepairType;

    /** 业务类型 */
    private String businessType;

    /** 机型 */
    private String machineModel;

    /** 产品类别 */
    private String productCategory;

    /** 产品类型 */
    private String productType;

    /** WBS元素 */
    private String wbsElement;

    /** 计划工单类型 */
    private String workOrderType;

    /** 计划组织 */
    private String planOrg;

    /** 计划数量 */
    private BigDecimal planQty;

    /** 数量单位 */
    private String qtyUnit;

    /** 完工数量 */
    private BigDecimal completedQty;

    /** 状态（CREATED/RELEASED） */
    private String status;

    /** 计划开始时间 */
    private LocalDateTime planStartTime;

    /** 计划完成时间 */
    private LocalDateTime planEndTime;

    /** 实际开始时间 */
    private LocalDateTime actualStartTime;

    /** 实际完成时间 */
    private LocalDateTime actualEndTime;
}
