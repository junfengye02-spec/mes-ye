package com.mes.dispatch.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 派工分派 DTO（通用，适用于人员/设备/班组分派）
 */
@Data
@Schema(description = "派工分派参数")
public class DispatchAssignDTO {

    @NotNull(message = "分派对象ID不能为空")
    @Schema(description = "分派对象ID")
    private Long assigneeId;

    @NotBlank(message = "分派对象编码不能为空")
    @Schema(description = "分派对象编码")
    private String assigneeCode;

    @Schema(description = "分派对象名称")
    private String assigneeName;

    @Schema(description = "分派数量")
    private BigDecimal assignedQty;

    @Schema(description = "数量单位")
    private String qtyUnit;
}
