package com.mes.workorder.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "约束关系视图对象")
public class WorkOrderConstraintVO {

    private Long id;
    private Long workOrderId;
    private String constraintType;
    private Long relatedWorkOrderId;
    private Long relatedTaskId;
    private String remark;
}
