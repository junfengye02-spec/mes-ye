package com.mes.quality.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单开工检查查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工单开工检查查询参数")
public class OrderStartCheckQuery extends PageQuery {

    @Schema(description = "工单ID")
    private Long workOrderId;

    @Schema(description = "工单号")
    private String workOrderNo;

    @Schema(description = "检查状态")
    private String checkStatus;
}
