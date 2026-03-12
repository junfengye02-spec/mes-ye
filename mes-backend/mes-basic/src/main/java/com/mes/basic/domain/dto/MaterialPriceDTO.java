package com.mes.basic.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 物料价格新增/编辑 DTO
 */
@Data
@Schema(description = "物料价格请求参数")
public class MaterialPriceDTO {

    @NotNull(message = "物料ID不能为空")
    @Schema(description = "物料ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long materialId;

    @NotNull(message = "物料单价不能为空")
    @DecimalMin(value = "0", message = "物料单价不能为负数")
    @Schema(description = "物料单价", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal unitPrice;

    @Schema(description = "单位")
    private String unit;
}
