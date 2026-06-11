package com.mes.quality.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 复检审批请求参数
 */
@Data
@Schema(description = "复检审批请求参数")
public class RecheckApproveDTO {

    @NotNull(message = "审批结果不能为空")
    @Schema(description = "是否批准", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean approved;
}
