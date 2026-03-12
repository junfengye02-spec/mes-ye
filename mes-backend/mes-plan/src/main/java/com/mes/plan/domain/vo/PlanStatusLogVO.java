package com.mes.plan.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 计划状态日志 VO
 */
@Data
@Schema(description = "计划状态日志视图对象")
public class PlanStatusLogVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "计划类型")
    private String planType;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "原状态")
    private String fromStatus;

    @Schema(description = "新状态")
    private String toStatus;

    @Schema(description = "动作")
    private String action;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "操作时间")
    private LocalDateTime operatedTime;

    @Schema(description = "说明")
    private String remark;
}
