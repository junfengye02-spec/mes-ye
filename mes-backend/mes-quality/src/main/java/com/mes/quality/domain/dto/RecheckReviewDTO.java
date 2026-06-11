package com.mes.quality.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 复检审核请求参数
 */
@Data
@Schema(description = "复检审核请求参数")
public class RecheckReviewDTO {

    @Schema(description = "审核人员")
    private String reviewer;

    @Schema(description = "审核日期")
    private LocalDate reviewDate;

    @Schema(description = "是否合理")
    private Integer isReasonable;
}
