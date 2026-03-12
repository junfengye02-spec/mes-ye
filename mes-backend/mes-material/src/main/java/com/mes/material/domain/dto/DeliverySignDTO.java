package com.mes.material.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "发货签收参数")
public class DeliverySignDTO {
    @Schema(description = "行号") private String lineNo;
    @Schema(description = "工单ID") private Long workOrderId;
    @Schema(description = "工单号") private String workOrderNo;
    @Schema(description = "物料ID") private Long materialId;
    @Schema(description = "物料编码") private String materialCode;
    @Schema(description = "物料名称") private String materialName;
    @Schema(description = "计划发货数量") private BigDecimal planDeliveryQty;
    @Schema(description = "待签收数量") private BigDecimal pendingSignQty;
    @Schema(description = "单位") private String unit;
    @Schema(description = "发货仓库") private String deliveryWarehouse;
    @Schema(description = "发货存储地点") private String deliveryLocation;
}
