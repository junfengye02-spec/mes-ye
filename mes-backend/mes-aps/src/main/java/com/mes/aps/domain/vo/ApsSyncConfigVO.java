package com.mes.aps.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "同步配置信息")
public class ApsSyncConfigVO {
    @Schema(description = "ID") private Long id;
    @Schema(description = "配置键") private String configKey;
    @Schema(description = "配置值") private String configValue;
    @Schema(description = "配置说明") private String configDesc;
    @Schema(description = "是否启用") private Integer enabled;
    @Schema(description = "更新时间") private LocalDateTime updatedTime;
}
