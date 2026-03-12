package com.mes.query.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "检验工作查询参数")
public class InspectionWorkQuery extends PageQuery {
    @Schema(description = "工作编号") private String workNo;
    @Schema(description = "工作名称") private String workName;
    @Schema(description = "工作状态") private String workStatus;
    @Schema(description = "工单ID") private Long workOrderId;
    @Schema(description = "检验类型") private String inspectCategory;
}
