package com.mes.quality.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 复检申请订单计划关联 VO
 */
@Data
@Schema(description = "复检订单计划关联信息")
public class RecheckOrderPlanVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "复检申请ID")
    private Long recheckId;

    @Schema(description = "订单计划ID")
    private Long orderPlanId;

    @Schema(description = "被关联对象")
    private String relatedObject;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}
