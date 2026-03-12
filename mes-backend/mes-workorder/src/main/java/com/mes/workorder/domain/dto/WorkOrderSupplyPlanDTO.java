package com.mes.workorder.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "供应计划参数")
public class WorkOrderSupplyPlanDTO {

    @Schema(description = "需求计划")
    private String demandPlanNo;

    @Schema(description = "供应计划")
    private String supplyPlanNo;

    @Schema(description = "供应数量")
    private BigDecimal supplyQty;

    @Schema(description = "计量单位")
    private String qtyUnit;

    @Schema(description = "计划组织")
    private String planOrg;

    @Schema(description = "编号")
    private String code;
}
