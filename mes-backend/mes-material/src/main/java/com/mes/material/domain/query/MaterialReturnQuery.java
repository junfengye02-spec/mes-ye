package com.mes.material.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "退料申请查询参数")
public class MaterialReturnQuery extends PageQuery {
    @Schema(description = "退料单号") private String returnNo;
    @Schema(description = "工单号") private String workOrderNo;
    @Schema(description = "工单ID") private Long workOrderId;
    @Schema(description = "状态") private String status;
}
