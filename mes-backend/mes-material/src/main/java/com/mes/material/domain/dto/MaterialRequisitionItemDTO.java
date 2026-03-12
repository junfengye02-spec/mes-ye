package com.mes.material.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "领料明细参数")
public class MaterialRequisitionItemDTO {
    @Schema(description = "物料ID") private Long materialId;
    @Schema(description = "物料编码") private String materialCode;
    @Schema(description = "物料名称") private String materialName;
    @Schema(description = "需求数量") private BigDecimal demandQty;
    @Schema(description = "本次领料数量") private BigDecimal issueQty;
    @Schema(description = "单位") private String unit;
    @Schema(description = "发货地点") private String issueLocation;
    @Schema(description = "需求时间") private LocalDateTime demandTime;
    @Schema(description = "说明") private String description;
}
