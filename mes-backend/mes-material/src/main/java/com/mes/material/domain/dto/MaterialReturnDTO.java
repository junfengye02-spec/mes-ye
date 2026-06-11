package com.mes.material.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "退料申请参数")
public class MaterialReturnDTO {
    @Schema(description = "退料单号（留空自动生成）") private String returnNo;
    @NotNull(message = "工单ID不能为空")
    @Schema(description = "工单ID", requiredMode = Schema.RequiredMode.REQUIRED) private Long workOrderId;
    @Schema(description = "工单号") private String workOrderNo;
    @Schema(description = "订单号") private String orderNo;
    @Schema(description = "产品编码") private String productCode;
    @Schema(description = "产品名称") private String productName;
    @Schema(description = "项目") private String projectName;
    @Schema(description = "WBS元素") private String wbsElement;
    @Schema(description = "新制维修类型") private String newOrRepairType;
    @JsonAlias("workType")
    @Schema(description = "业务类型") private String businessType;
    @JsonAlias("pcclFlow")
    @Schema(description = "流程编码") private String flowCode;
    @Schema(description = "计划数量") private BigDecimal planQty;
    @Schema(description = "完工数量") private BigDecimal completedQty;
}
