package com.mes.process.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 指导书新增/编辑 DTO
 */
@Data
@Schema(description = "指导书请求参数")
public class WorkInstructionDTO {

    @NotBlank(message = "指导书编号不能为空")
    @Schema(description = "指导书编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String instructionCode;

    @Schema(description = "等级")
    private String level;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "人员列表")
    private List<WorkInstructionPersonDTO> persons;
}
