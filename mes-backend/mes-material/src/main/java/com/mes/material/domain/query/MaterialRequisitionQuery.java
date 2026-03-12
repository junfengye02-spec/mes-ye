package com.mes.material.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "领料申请查询参数")
public class MaterialRequisitionQuery extends PageQuery {
    @Schema(description = "领料单号") private String requisitionNo;
    @Schema(description = "工单号") private String workOrderNo;
    @Schema(description = "工单ID") private Long workOrderId;
    @Schema(description = "产品编码") private String productCode;
    @Schema(description = "状态") private String status;
}
