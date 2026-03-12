package com.mes.material.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 领料单管理（按物料）请求参数
 */
@Data
@Schema(description = "领料单管理请求参数")
public class RequisitionOrderDTO {
    @Schema(description = "上级节点") private String parentNode;
    @Schema(description = "发货申请") private String deliveryRequestNo;
    @Schema(description = "行号") private String lineNo;
    @Schema(description = "工单ID") private Long workOrderId;
    @Schema(description = "工单号") private String workOrderNo;
    @Schema(description = "已制发货单") private Integer deliveryOrderCreated;
    @NotNull(message = "物料ID不能为空")
    @Schema(description = "物料ID", requiredMode = Schema.RequiredMode.REQUIRED) private Long materialId;
    @Schema(description = "物料编码") private String materialCode;
    @Schema(description = "物料名称") private String materialName;
    @Schema(description = "仓库已发货") private Integer warehouseDelivered;
    @NotNull(message = "领料数量不能为空")
    @Schema(description = "领料数量", requiredMode = Schema.RequiredMode.REQUIRED) private BigDecimal requisitionQty;
    @Schema(description = "状态") private String status;
    @Schema(description = "发货仓库") private String deliveryWarehouse;
    @Schema(description = "发货库位") private String deliveryLocation;
    @Schema(description = "工作ID") private Long workId;
    @Schema(description = "工位") private String workStation;
    @Schema(description = "物料需求ID") private Long materialDemandId;
}
