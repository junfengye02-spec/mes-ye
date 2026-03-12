package com.mes.workorder.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "供应计划视图对象")
public class WorkOrderSupplyPlanVO {

    private Long id;
    private Long workOrderId;
    private String demandPlanNo;
    private String supplyPlanNo;
    private BigDecimal supplyQty;
    private String qtyUnit;
    private String planOrg;
    private BigDecimal completedQty;
    private String code;
}
