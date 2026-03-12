package com.mes.process.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 制造BOM查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "制造BOM查询参数")
public class ManufacturingBomQuery extends PageQuery {

    @Schema(description = "BOM编码")
    private String bomCode;

    @Schema(description = "BOM名称")
    private String bomName;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "状态")
    private String status;
}
