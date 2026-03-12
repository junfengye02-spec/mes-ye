package com.mes.material.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "领料单管理信息")
public class RequisitionOrderVO {
    @Schema(description = "ID") private Long id;
    @Schema(description = "上级节点") private String parentNode;
    @Schema(description = "发货申请") private String deliveryRequestNo;
    @Schema(description = "行号") private String lineNo;
    @Schema(description = "工单ID") private Long workOrderId;
    @Schema(description = "工单号") private String workOrderNo;
    @Schema(description = "已制发货单") private Integer deliveryOrderCreated;
    @Schema(description = "物料编码") private String materialCode;
    @Schema(description = "物料名称") private String materialName;
    @Schema(description = "仓库已发货") private Integer warehouseDelivered;
    @Schema(description = "领料数量") private BigDecimal requisitionQty;
    @Schema(description = "状态") private String status;
    @Schema(description = "创建时间") private LocalDateTime createdTime;
}
