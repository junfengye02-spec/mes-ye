package com.mes.material.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "完工入库申请查询参数")
public class ReceiptRequestQuery extends PageQuery {
    @Schema(description = "申请单号") private String requestNo;
    @Schema(description = "入库类型") private String receiptType;
    @Schema(description = "工单ID") private Long workOrderId;
    @Schema(description = "状态") private String status;
}
