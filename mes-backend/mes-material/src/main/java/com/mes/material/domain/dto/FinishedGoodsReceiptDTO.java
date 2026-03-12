package com.mes.material.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "完工入库单请求参数")
public class FinishedGoodsReceiptDTO {
    @Schema(description = "收货单号（留空自动生成）") private String receiptNo;
    @Schema(description = "收货类型") private String receiptType;
    @Schema(description = "仓库") private String warehouse;
    @Schema(description = "移动类型") private String movementType;
    @Schema(description = "计划收货时间") private LocalDateTime planReceiptTime;
    @Schema(description = "入库明细") private List<FinishedGoodsReceiptItemDTO> items;
}
