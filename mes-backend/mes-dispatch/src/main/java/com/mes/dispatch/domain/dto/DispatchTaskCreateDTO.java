package com.mes.dispatch.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 派工任务创建 DTO
 * <p>用于 /dispatch/task/create 接口，支持手动创建派工单</p>
 */
@Data
@Schema(description = "派工任务创建参数")
public class DispatchTaskCreateDTO {

    @NotNull(message = "工单ID不能为空")
    @Schema(description = "工单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workOrderId;

    @Schema(description = "工作清单ID（可选，关联工单时可填）")
    private Long workOrderTaskId;

    @NotBlank(message = "订单编号不能为空")
    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderNo;

    @NotBlank(message = "工序号不能为空")
    @Schema(description = "工序号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String processNo;

    @NotBlank(message = "工作名称不能为空")
    @Schema(description = "工作名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workName;

    @Schema(description = "计划工作中心 ID")
    private Long planWorkCenterId;

    @Schema(description = "序列号")
    private String serialNo;

    @Schema(description = "项目名称")
    private String projectName;

    @NotNull(message = "计划数量不能为空")
    @Positive(message = "计划数量必须大于 0")
    @Schema(description = "计划数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal planQty;

    @NotBlank(message = "数量单位不能为空")
    @Schema(description = "数量单位", requiredMode = Schema.RequiredMode.REQUIRED)
    private String qtyUnit;

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;
}
