package com.mes.process.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BOM版本日志返回 VO
 */
@Data
@Schema(description = "BOM版本日志信息")
public class BomVersionLogVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "BOM主表ID")
    private Long bomId;

    @Schema(description = "原版本")
    private String fromVersion;

    @Schema(description = "新版本")
    private String toVersion;

    @Schema(description = "动作")
    private String action;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "操作时间")
    private LocalDateTime operatedTime;

    @Schema(description = "变更摘要")
    private String changeSummary;
}
