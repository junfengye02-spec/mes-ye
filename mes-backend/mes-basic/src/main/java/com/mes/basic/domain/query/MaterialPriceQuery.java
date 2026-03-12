package com.mes.basic.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 物料价格查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "物料价格查询参数")
public class MaterialPriceQuery extends PageQuery {

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;
}
