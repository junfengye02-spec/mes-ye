package com.mes.aps.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "手动触发同步请求参数")
public class ApsSyncRequestDTO {
    @Schema(description = "同步类型（ORDER/TASK/RESOURCE/CALENDAR/ALL）") private String syncType;
    @Schema(description = "同步方向（DOWNSTREAM/UPSTREAM）") private String syncDirection;
    @Schema(description = "是否全量同步") private Boolean fullSync;
}
