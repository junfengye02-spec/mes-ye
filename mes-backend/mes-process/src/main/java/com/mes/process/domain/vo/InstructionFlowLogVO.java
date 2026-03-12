package com.mes.process.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 指示书流程日志返回 VO
 */
@Data
@Schema(description = "指示书流程日志信息")
public class InstructionFlowLogVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "指示书ID")
    private Long instructionId;

    @Schema(description = "动作")
    private String action;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "操作时间")
    private LocalDateTime operatedTime;

    @Schema(description = "说明")
    private String detail;
}
