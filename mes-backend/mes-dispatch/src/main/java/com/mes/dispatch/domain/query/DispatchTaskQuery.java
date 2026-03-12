package com.mes.dispatch.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 派工任务查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "派工任务查询参数")
public class DispatchTaskQuery extends PageQuery {

    @Schema(description = "工单ID")
    private Long workOrderId;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "工序号")
    private String processNo;

    @Schema(description = "分派状态")
    private String dispatchStatus;
}
