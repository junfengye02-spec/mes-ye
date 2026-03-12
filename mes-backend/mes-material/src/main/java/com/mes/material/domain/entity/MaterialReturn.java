package com.mes.material.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产退料申请表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_material_return")
public class MaterialReturn extends BaseEntity {

    private String returnNo;
    private Long workOrderId;
    private String workOrderNo;
    private String orderNo;
    private String productCode;
    private String productName;
    private String projectName;
    private String wbsElement;
    private String newOrRepairType;
    private String workType;
    private String machineModel;
    private String productCategory;
    private String productType;
    private BigDecimal planQty;
    private BigDecimal completedQty;
    private String factoryOrg;
    private String planOrg;
    private String mainOrg;
    private Long planWorkCenterId;
    private String status;
    private String flowStatus;
    private String expandStatus;
    private Integer isOrder;
    private String pcclFlow;
    private LocalDateTime planStartTime;
    private LocalDateTime planEndTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
}
