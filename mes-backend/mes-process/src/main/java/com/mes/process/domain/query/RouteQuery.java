package com.mes.process.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工艺路线查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工艺路线查询参数")
public class RouteQuery extends PageQuery {

    @Schema(description = "路线编码")
    private String routeCode;

    @Schema(description = "路线名称")
    private String routeName;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "机型")
    private String machineModel;

    @Schema(description = "状态")
    private String status;
}
