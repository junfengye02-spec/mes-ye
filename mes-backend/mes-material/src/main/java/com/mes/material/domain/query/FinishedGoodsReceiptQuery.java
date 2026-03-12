package com.mes.material.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "完工入库单查询参数")
public class FinishedGoodsReceiptQuery extends PageQuery {
    @Schema(description = "收货单号") private String receiptNo;
    @Schema(description = "收货类型") private String receiptType;
    @Schema(description = "状态") private String status;
}
