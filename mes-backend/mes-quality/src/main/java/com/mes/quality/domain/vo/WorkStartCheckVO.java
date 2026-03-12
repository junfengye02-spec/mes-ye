package com.mes.quality.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作开工检查 VO
 */
@Data
@Schema(description = "工作开工检查信息")
public class WorkStartCheckVO {

    @Schema(description = "ID")
    private Long id;
    @Schema(description = "工作编号")
    private String workNo;
    @Schema(description = "工作清单ID")
    private Long workOrderTaskId;
    @Schema(description = "工单ID")
    private Long workOrderId;
    @Schema(description = "工单号")
    private String workOrderNo;
    @Schema(description = "检查项目")
    private String checkItem;
    @Schema(description = "检查结果")
    private String checkResult;
    @Schema(description = "检查状态")
    private String checkStatus;
    @Schema(description = "检查备注")
    private String checkRemark;
    @Schema(description = "检查人")
    private String checker;
    @Schema(description = "检查时间")
    private LocalDateTime checkTime;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建人")
    private String createdBy;
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}
