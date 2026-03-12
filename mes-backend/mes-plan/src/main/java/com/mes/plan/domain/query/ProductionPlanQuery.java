package com.mes.plan.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生产计划查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "生产计划查询参数")
public class ProductionPlanQuery extends PageQuery {

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "类型")
    private String workType;

    @Schema(description = "机型")
    private String machineModel;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "订单计划ID")
    private Long orderPlanId;
}
