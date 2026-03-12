package com.mes.workorder.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "输出物料参数")
public class WorkOrderOutputMaterialDTO {

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "产出数量")
    private BigDecimal outputQty;

    @Schema(description = "数量单位")
    private String qtyUnit;
}
