package com.mes.quality.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 工作开工检查 DTO
 */
@Data
@Schema(description = "工作开工检查请求参数")
public class WorkStartCheckDTO {

    @Schema(description = "工作编号")
    private String workNo;

    @Schema(description = "工作清单ID")
    private Long workOrderTaskId;

    @NotNull(message = "工单ID不能为空")
    @Schema(description = "工单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workOrderId;

    @Schema(description = "工单号")
    private String workOrderNo;

    @NotBlank(message = "检查项目不能为空")
    @Schema(description = "检查项目", requiredMode = Schema.RequiredMode.REQUIRED)
    private String checkItem;

    @Schema(description = "检查结果")
    private String checkResult;

    @NotBlank(message = "检查状态不能为空")
    @Schema(description = "检查状态（PASSED/FAILED）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String checkStatus;

    @Schema(description = "检查备注")
    private String checkRemark;

    @Schema(description = "备注")
    private String remark;
}
