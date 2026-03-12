package com.mes.workorder.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 工作清单 DTO
 */
@Data
@Schema(description = "工作清单参数")
public class WorkOrderTaskDTO {

    @Schema(description = "工序号")
    private String taskNo;

    @Schema(description = "工作名称")
    private String taskName;

    @Schema(description = "计划工作中心ID")
    private Long planWorkCenterId;

    @Schema(description = "计划数量")
    private BigDecimal planQty;

    @Schema(description = "数量单位")
    private String qtyUnit;

    @Schema(description = "顺序号")
    private Integer sequenceNo;

    @Schema(description = "序列号")
    private String serialNo;

    @Schema(description = "项目")
    private String projectName;
}
