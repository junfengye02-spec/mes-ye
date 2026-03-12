package com.mes.material.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "入库明细信息")
public class FinishedGoodsReceiptItemVO {
    @Schema(description = "ID") private Long id;
    @Schema(description = "入库单ID") private Long receiptId;
    @Schema(description = "编码") private String itemCode;
    @Schema(description = "工单ID") private Long workOrderId;
    @Schema(description = "工单号") private String workOrderNo;
    @Schema(description = "物料编码") private String materialCode;
    @Schema(description = "物料名称") private String materialName;
    @Schema(description = "收货数量") private BigDecimal receiptQty;
    @Schema(description = "单位") private String unit;
    @Schema(description = "存储地点") private String storageLocation;
    @Schema(description = "差异数量") private BigDecimal varianceQty;
    @Schema(description = "差异原因") private String varianceReason;
}
