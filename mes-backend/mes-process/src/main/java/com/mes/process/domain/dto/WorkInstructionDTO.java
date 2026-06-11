package com.mes.process.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 指导书新增/编辑 DTO
 * <p>面向可复用的标准作业指导书模板，而不是单次工单执行单据。</p>
 */
@Data
@Schema(description = "指导书请求参数（可复用作业指导书模板）")
public class WorkInstructionDTO {

    @NotBlank(message = "指导书编号不能为空")
    @Schema(description = "指导书编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String instructionCode;

    @NotBlank(message = "指导书名称不能为空")
    @Schema(description = "指导书名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String instructionName;

    @Schema(description = "关联工序ID")
    private Long processId;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "作业内容")
    private String content;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "等级")
    private String level;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "人员列表")
    private List<WorkInstructionPersonDTO> persons;
}
