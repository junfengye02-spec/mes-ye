package com.mes.quality.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 复检申请查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "复检申请查询参数")
public class RecheckRequestQuery extends PageQuery {

    @Schema(description = "项目编码")
    private String projectCode;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "生产订单")
    private String productionOrderNo;

    @Schema(description = "状态")
    private String status;
}
