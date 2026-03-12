package com.mes.aps.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "同步配置请求参数")
public class ApsSyncConfigDTO {
    @NotBlank(message = "配置键不能为空")
    @Schema(description = "配置键") private String configKey;
    @NotBlank(message = "配置值不能为空")
    @Schema(description = "配置值") private String configValue;
    @Schema(description = "配置说明") private String configDesc;
    @Schema(description = "是否启用") private Integer enabled;
}
