package com.mes.aps.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "同步日志查询参数")
public class ApsSyncLogQuery extends PageQuery {
    @Schema(description = "同步方向") private String syncDirection;
    @Schema(description = "同步类型") private String syncType;
    @Schema(description = "同步状态") private String status;
    @Schema(description = "开始时间起") private LocalDateTime startTimeFrom;
    @Schema(description = "开始时间止") private LocalDateTime startTimeTo;
}
