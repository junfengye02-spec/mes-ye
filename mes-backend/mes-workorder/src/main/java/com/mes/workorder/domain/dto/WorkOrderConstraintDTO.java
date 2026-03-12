package com.mes.workorder.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "约束关系参数")
public class WorkOrderConstraintDTO {

    @Schema(description = "约束类型")
    private String constraintType;

    @Schema(description = "关联工单ID")
    private Long relatedWorkOrderId;

    @Schema(description = "关联工作清单ID")
    private Long relatedTaskId;

    @Schema(description = "说明")
    private String remark;
}
