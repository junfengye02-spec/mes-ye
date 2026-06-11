package com.mes.material.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "领料申请请求参数")
public class MaterialRequisitionDTO {
    @Schema(description = "领料单号（留空自动生成）") private String requisitionNo;
    @NotNull(message = "工单ID不能为空")
    @Schema(description = "工单ID", requiredMode = Schema.RequiredMode.REQUIRED) private Long workOrderId;
    @Schema(description = "工单号") private String workOrderNo;
    @Schema(description = "产品编码") private String productCode;
    @Schema(description = "产品名称") private String productName;
    @Schema(description = "计划数量") private BigDecimal planQty;
    @Schema(description = "实际数量") private BigDecimal actualQty;
    @Schema(description = "合格数量") private BigDecimal qualifiedQty;
    @Schema(description = "数量单位") private String qtyUnit;
    @Schema(description = "主制组织") private String mainOrg;
    @Schema(description = "计划开始时间") private LocalDateTime planStartTime;
    @Schema(description = "计划结束时间") private LocalDateTime planEndTime;
    @Schema(description = "实际开始时间") private LocalDateTime actualStartTime;
    @Schema(description = "实际结束时间") private LocalDateTime actualEndTime;
    @Schema(description = "销售订单行") private String salesOrderLine;
    @Schema(description = "项目") private String projectName;
    @Schema(description = "WBS元素") private String wbsElement;
    @Schema(description = "领料明细") private List<MaterialRequisitionItemDTO> items;
}
