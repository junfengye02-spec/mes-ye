package com.mes.process.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 指导书返回 VO
 * <p>用于展示可复用的标准作业指导书模板及其适用人员信息。</p>
 */
@Data
@Schema(description = "指导书信息（可复用作业指导书模板）")
public class WorkInstructionVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "指导书编号")
    private String instructionCode;

    @Schema(description = "指导书名称")
    private String instructionName;

    @Schema(description = "关联工序ID")
    private Long processId;

    @Schema(description = "工序名称")
    private String processName;

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
