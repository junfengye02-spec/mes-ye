package com.mes.material.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "完工入库单信息")
public class FinishedGoodsReceiptVO {
    @Schema(description = "ID") private Long id;
    @Schema(description = "收货单号") private String receiptNo;
    @Schema(description = "收货类型") private String receiptType;
    @Schema(description = "仓库") private String warehouse;
    @Schema(description = "移动类型") private String movementType;
    @Schema(description = "计划收货时间") private LocalDateTime planReceiptTime;
    @Schema(description = "实际收货时间") private LocalDateTime actualReceiptTime;
    @Schema(description = "状态") private String status;
    @Schema(description = "入库明细") private List<FinishedGoodsReceiptItemVO> items;
    @Schema(description = "创建人") private String createdBy;
    @Schema(description = "创建时间") private LocalDateTime createdTime;
}
