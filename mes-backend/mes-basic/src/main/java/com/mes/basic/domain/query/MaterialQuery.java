package com.mes.basic.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 物料档案查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "物料档案查询参数")
public class MaterialQuery extends PageQuery {

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "物料类型")
    private String materialType;

    @Schema(description = "图号")
    private String drawingNo;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "机型")
    private String machineModel;
}
