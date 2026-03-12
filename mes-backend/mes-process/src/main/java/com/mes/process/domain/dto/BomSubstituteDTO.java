package com.mes.process.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * BOM替代料 DTO
 */
@Data
@Schema(description = "BOM替代料请求参数")
public class BomSubstituteDTO {

    @Schema(description = "主物料ID")
    private Long mainMaterialId;

    @Schema(description = "主物料编码")
    private String mainMaterialCode;

    @Schema(description = "替代物料ID")
    private Long substituteMaterialId;

    @Schema(description = "替代物料编码")
    private String substituteMaterialCode;

    @Schema(description = "替代物料名称")
    private String substituteMaterialName;

    @Schema(description = "替代优先级")
    private Integer priority;

    @Schema(description = "替代比例")
    private BigDecimal substituteRatio;

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期")
    private LocalDate expiryDate;

    @Schema(description = "备注")
    private String remark;
}
