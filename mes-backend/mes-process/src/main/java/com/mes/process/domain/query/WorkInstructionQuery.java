package com.mes.process.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 指导书查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "指导书查询参数")
public class WorkInstructionQuery extends PageQuery {

    @Schema(description = "指导书编号")
    private String instructionCode;

    @Schema(description = "指导书名称")
    private String instructionName;

    @Schema(description = "关联工序ID")
    private Long processId;

    @Schema(description = "等级")
    private String level;

    @Schema(description = "状态")
    private String status;
}
