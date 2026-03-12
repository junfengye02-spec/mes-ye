package com.mes.workorder.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "检验项目参数")
public class WorkOrderQualityItemDTO {

    @Schema(description = "检验项目编号")
    private String qualityItemCode;

    @Schema(description = "检验项目名称")
    private String qualityItemName;

    @Schema(description = "检验要求")
    private String requirement;
}
