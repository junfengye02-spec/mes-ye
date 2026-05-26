package com.mes.process.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工艺路线步骤返回值。
 */
@Data
@Schema(description = "工艺路线步骤")
public class RouteStepVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "路线ID")
    private Long routeId;

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

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "修改时间")
    private LocalDateTime updatedTime;
}
