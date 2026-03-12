package com.mes.process.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 指导书返回 VO
 */
@Data
@Schema(description = "指导书信息")
public class WorkInstructionVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "指导书编号")
    private String instructionCode;

    @Schema(description = "等级")
    private String level;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "人员列表")
    private List<WorkInstructionPersonVO> persons;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "修改人")
    private String updatedBy;

    @Schema(description = "修改时间")
    private LocalDateTime updatedTime;
}
