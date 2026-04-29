package com.mes.dispatch.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 派工指派 DTO（统一入口）
 * <p>用于 /dispatch/task/assign 接口，支持按 人员/设备/班组 批量指派</p>
 * <p>assignType 取值：</p>
 * <ul>
 *   <li>PERSON 按人员</li>
 *   <li>EQUIPMENT 或 DEVICE 按设备（EQUIPMENT 是外部约定同义词，内部归一化为 DEVICE）</li>
 *   <li>TEAM 按班组</li>
 * </ul>
 */
@Data
@Schema(description = "派工指派参数")
public class DispatchTaskAssignDTO {

    @NotNull(message = "派工任务ID不能为空")
    @Schema(description = "派工任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long taskId;

    @NotBlank(message = "分派类型不能为空")
    @Pattern(regexp = "^(PERSON|EQUIPMENT|DEVICE|TEAM)$",
            message = "分派类型必须为 PERSON / EQUIPMENT / DEVICE / TEAM 之一")
    @Schema(description = "分派类型", example = "PERSON", requiredMode = Schema.RequiredMode.REQUIRED)
    private String assignType;

    @NotEmpty(message = "分派对象列表不能为空")
    @Schema(description = "分派对象 ID 列表（可以一次派多个）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> assigneeIds;

    @Schema(description = "分派对象编码列表（与 assigneeIds 一一对应，可选）")
    private List<String> assigneeCodes;

    @Schema(description = "分派对象名称列表（与 assigneeIds 一一对应，可选）")
    private List<String> assigneeNames;

    @Schema(description = "分派给每个对象的数量（可选，为空则不细分）")
    private BigDecimal assignedQty;

    @Schema(description = "数量单位")
    private String qtyUnit;
}
