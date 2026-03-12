package com.mes.aps.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "同步结果")
public class ApsSyncResultVO {
    @Schema(description = "同步批次号") private String batchId;
    @Schema(description = "同步状态") private String status;
    @Schema(description = "总量") private int totalCount;
    @Schema(description = "成功数") private int successCount;
    @Schema(description = "失败数") private int failCount;
    @Schema(description = "耗时（毫秒）") private long durationMs;
    @Schema(description = "消息") private String message;
}
