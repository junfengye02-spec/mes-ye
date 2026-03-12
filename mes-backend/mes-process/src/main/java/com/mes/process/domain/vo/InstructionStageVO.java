package com.mes.process.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 指示书阶段返回 VO
 */
@Data
@Schema(description = "指示书阶段信息")
public class InstructionStageVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "指示书ID")
    private Long instructionId;

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
