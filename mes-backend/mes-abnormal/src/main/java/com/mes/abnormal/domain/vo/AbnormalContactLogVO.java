package com.mes.abnormal.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异常联络单状态日志返回 VO
 */
@Data
@Schema(description = "异常联络单状态日志")
public class AbnormalContactLogVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "异常联络单ID")
    private Long contactId;

    @Schema(description = "原状态")
    private String fromStatus;

    @Schema(description = "新状态")
    private String toStatus;

    @Schema(description = "动作")
    private String action;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "操作时间")
    private LocalDateTime operatedTime;

    @Schema(description = "说明")
    private String remark;
}
