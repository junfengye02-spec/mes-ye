package com.mes.process.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 工艺路线步骤请求参数。
 */
@Data
@Schema(description = "工艺路线步骤请求参数")
public class RouteStepDTO {

    @Schema(description = "顺序号")
    private Integer sequenceNo;

    @Schema(description = "工序ID")
    private Long processId;

    @Schema(description = "工序号")
    private String processNo;

    @Schema(description = "工序名称")
    private String processName;

    @Schema(description = "工作中心ID")
    private Long workCenterId;

    @Schema(description = "标准处理时间")
    private BigDecimal handleTime;

    @Schema(description = "前置步骤ID")
    private Long predecessorStepId;

    @Schema(description = "是否并行")
    private Integer parallelFlag;

    @Schema(description = "是否可选")
    private Integer optionalFlag;

    @Schema(description = "备注")
    private String remark;
}
