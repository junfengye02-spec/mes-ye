package com.mes.dispatch.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 派工任务更新 DTO
 * <p>用于 /dispatch/task/update 接口，只能修改未开工（UNASSIGNED/ASSIGNED）的任务</p>
 */
@Data
@Schema(description = "派工任务更新参数")
public class DispatchTaskUpdateDTO {

    @NotNull(message = "派工任务ID不能为空")
    @Schema(description = "派工任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "工序号")
    private String processNo;

    @Schema(description = "工作名称")
    private String workName;

    @Schema(description = "计划工作中心 ID")
    private Long planWorkCenterId;

    @Schema(description = "序列号")
    private String serialNo;

    @Schema(description = "项目名称")
    private String projectName;

    @Positive(message = "计划数量必须大于 0")
    @Schema(description = "计划数量")
    private BigDecimal planQty;

    @Schema(description = "数量单位")
    private String qtyUnit;

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;
}
