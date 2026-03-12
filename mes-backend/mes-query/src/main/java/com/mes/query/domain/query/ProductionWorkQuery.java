package com.mes.query.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "生产工作查询参数")
public class ProductionWorkQuery extends PageQuery {
    @Schema(description = "工作编号") private String workNo;
    @Schema(description = "工作名称") private String workName;
    @Schema(description = "工单号") private String workOrderNo;
    @Schema(description = "工单ID") private Long workOrderId;
}
