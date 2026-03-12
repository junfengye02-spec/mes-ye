package com.mes.material.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 生产领料单管理表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_requisition_order")
public class RequisitionOrder extends BaseEntity {

    private String parentNode;
    private String deliveryRequestNo;
    private String lineNo;
    private Long workOrderId;
    private String workOrderNo;
    private Integer deliveryOrderCreated;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Integer warehouseDelivered;
    private BigDecimal requisitionQty;
    private String status;
    private String deliveryWarehouse;
    private String deliveryLocation;
    private Long workId;
    private String workStation;
    private Long materialDemandId;
}
