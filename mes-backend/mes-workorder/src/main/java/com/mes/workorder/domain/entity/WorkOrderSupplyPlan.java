package com.mes.workorder.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 供应计划表实体
 */
@Data
@TableName("mes_work_order_supply_plan")
public class WorkOrderSupplyPlan implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long workOrderId;

    /** 需求计划 */
    private String demandPlanNo;

    /** 供应计划 */
    private String supplyPlanNo;

    /** 供应数量 */
    private BigDecimal supplyQty;

    /** 计量单位 */
    private String qtyUnit;

    /** 计划组织 */
    private String planOrg;

    /** 完工数量 */
    private BigDecimal completedQty;

    /** 编号 */
    private String code;
}
