package com.mes.quality.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 复检申请订单计划关联 DTO
 */
@Data
@Schema(description = "复检订单计划关联参数")
public class RecheckOrderPlanDTO {

    @Schema(description = "订单计划ID")
    private Long orderPlanId;

    @Schema(description = "被关联对象")
    private String relatedObject;
}
