package com.mes.material.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "完工入库申请参数")
public class ReceiptRequestDTO {
    @Schema(description = "申请单号（留空自动生成）") private String requestNo;
    @Schema(description = "入库类型") private String receiptType;
    @Schema(description = "工单ID") private Long workOrderId;
    @Schema(description = "工单号") private String workOrderNo;
    @Schema(description = "项目") private String projectName;
    @Schema(description = "WBS元素") private String wbsElement;
    @Schema(description = "物料ID") private Long materialId;
    @Schema(description = "物料编码") private String materialCode;
    @Schema(description = "物料名称") private String materialName;
    @Schema(description = "序列号") private String serialNo;
    @Schema(description = "数量") private BigDecimal qty;
    @Schema(description = "合格数量") private BigDecimal qualifiedQty;
    @Schema(description = "不合格数量") private BigDecimal unqualifiedQty;
    @Schema(description = "单位") private String unit;
    @Schema(description = "说明") private String description;
    @Schema(description = "计划入库时间") private LocalDateTime planReceiptTime;
}
