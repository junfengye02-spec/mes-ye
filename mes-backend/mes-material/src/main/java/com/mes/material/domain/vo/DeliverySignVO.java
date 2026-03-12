package com.mes.material.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "发货签收信息")
public class DeliverySignVO {
    @Schema(description = "ID") private Long id;
    @Schema(description = "行号") private String lineNo;
    @Schema(description = "工单ID") private Long workOrderId;
    @Schema(description = "工单号") private String workOrderNo;
    @Schema(description = "物料编码") private String materialCode;
    @Schema(description = "物料名称") private String materialName;
    @Schema(description = "计划发货数量") private BigDecimal planDeliveryQty;
    @Schema(description = "待签收数量") private BigDecimal pendingSignQty;
    @Schema(description = "单位") private String unit;
    @Schema(description = "发货仓库") private String deliveryWarehouse;
    @Schema(description = "发货人") private String deliverer;
    @Schema(description = "发货时间") private LocalDateTime deliveryTime;
}
