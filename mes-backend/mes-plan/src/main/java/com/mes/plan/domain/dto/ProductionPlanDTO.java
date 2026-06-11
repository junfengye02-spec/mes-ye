package com.mes.plan.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产计划 DTO
 */
@Data
@Schema(description = "生产计划创建/修改参数")
public class ProductionPlanDTO {

    @NotNull(message = "订单计划ID不能为空")
    @Schema(description = "订单计划ID")
    private Long orderPlanId;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "新制维修类型")
    private String newOrRepairType;

    @JsonAlias("workType")
    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "机型")
    private String machineModel;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "产品类型")
    private String productType;

    @Schema(description = "WBS元素")
    private String wbsElement;

    @Schema(description = "计划工单类型")
    private String workOrderType;

    @Schema(description = "计划组织")
    private String planOrg;

    @DecimalMin(value = "0.0001", message = "计划数量必须大于0")
    @Schema(description = "计划数量")
    private BigDecimal planQty;

    @Schema(description = "数量单位")
    private String qtyUnit;

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划完成时间")
    private LocalDateTime planEndTime;
}
