package com.mes.aps.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "同步日志信息")
public class ApsSyncLogVO {
    @Schema(description = "ID") private Long id;
    @Schema(description = "同步批次号") private String batchId;
    @Schema(description = "同步方向") private String syncDirection;
    @Schema(description = "同步类型") private String syncType;
    @Schema(description = "数据总量") private Integer totalCount;
    @Schema(description = "成功数量") private Integer successCount;
    @Schema(description = "失败数量") private Integer failCount;
    @Schema(description = "同步状态") private String status;
    @Schema(description = "开始时间") private LocalDateTime startTime;
    @Schema(description = "结束时间") private LocalDateTime endTime;
    @Schema(description = "耗时（毫秒）") private Long durationMs;
    @Schema(description = "错误信息") private String errorMessage;
    @Schema(description = "创建时间") private LocalDateTime createdTime;
}
