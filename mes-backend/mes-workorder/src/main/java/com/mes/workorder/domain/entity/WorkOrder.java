package com.mes.workorder.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产工单主表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_work_order")
public class WorkOrder extends BaseEntity {

    /** 工单号 */
    private String workOrderNo;

    /** 工单类型 */
    private String workOrderType;

    /** 生产计划 */
    private String productionPlanNo;

    /** 订单计划 */
    private String orderPlanNo;

    /** 订单编号 */
    private String orderNo;

    /** 产品编码 */
    private String productCode;

    /** 产品名称 */
    private String productName;

    /** 主产品 */
    private String mainProduct;

    /** 机型 */
    private String machineModel;

    /** 产品类别 */
    private String productCategory;

    /** 产品类型 */
    private String productType;

    /** 制造BOM */
    private String bomCode;

    /** 项目 */
    private String projectName;

    /** WBS元素 */
    private String wbsElement;

    /** 新制维修类型 */
    private String newOrRepairType;

    /** 类型 */
    private String workType;

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

    /** 指定工作中心 */
    private Long specifiedWorkCenterId;

    /** 状态 */
    private String status;

    /** 计划开始时间 */
    private LocalDateTime planStartTime;

    /** 计划结束时间 */
    private LocalDateTime planEndTime;

    /** 实际开始时间 */
    private LocalDateTime actualStartTime;

    /** 实际结束时间 */
    private LocalDateTime actualEndTime;

    /** 序列号 */
    private String serialNo;

    /** 特殊库存标识 */
    private String specialStockFlag;

    /** 交货地点 */
    private String deliveryLocation;

    /** 说明 */
    private String remark;
}
