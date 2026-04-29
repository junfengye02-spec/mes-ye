package com.mes.dispatch.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 派工任务完工 DTO
 * <p>用于 /dispatch/task/complete/&#123;id&#125; 接口，记录实际完成数量与质量结果</p>
 */
@Data
@Schema(description = "派工任务完工参数")
public class DispatchTaskCompleteDTO {

    @Schema(description = "实际开工时间（若 start 接口已填，可省略）")
    private LocalDateTime actualStartTime;

    @NotNull(message = "实际完工时间不能为空")
    @Schema(description = "实际完工时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime actualEndTime;

    @NotNull(message = "实际完成数量不能为空")
    @PositiveOrZero(message = "实际完成数量不能小于 0")
    @Schema(description = "实际完成数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal actualQty;

    @NotNull(message = "质量结果不能为空")
    @Pattern(regexp = "^(PASS|FAIL|NA)$", message = "质量结果必须为 PASS / FAIL / NA")
    @Schema(description = "质量结果：PASS=合格 / FAIL=不合格 / NA=不适用",
            example = "PASS", requiredMode = Schema.RequiredMode.REQUIRED)
    private String qualityResult;

    @Schema(description = "完工备注")
    private String remark;
}
