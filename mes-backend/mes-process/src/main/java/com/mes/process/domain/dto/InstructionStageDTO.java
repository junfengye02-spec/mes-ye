package com.mes.process.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 指示书阶段 DTO
 */
@Data
@Schema(description = "指示书阶段请求参数")
public class InstructionStageDTO {

    @Schema(description = "阶段")
    private String stage;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "要求纳期")
    private LocalDate requiredDate;

    @Schema(description = "实际纳期")
    private LocalDate actualDate;
}
