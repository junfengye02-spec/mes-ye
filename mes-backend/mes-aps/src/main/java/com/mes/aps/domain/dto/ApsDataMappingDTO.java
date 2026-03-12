package com.mes.aps.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "数据映射请求参数")
public class ApsDataMappingDTO {
    @NotBlank(message = "映射类型不能为空")
    @Schema(description = "映射类型") private String mappingType;
    @NotBlank(message = "MES编码不能为空")
    @Schema(description = "MES编码") private String mesCode;
    @Schema(description = "MES名称") private String mesName;
    @NotBlank(message = "APS编码不能为空")
    @Schema(description = "APS编码") private String apsCode;
    @Schema(description = "APS名称") private String apsName;
    @Schema(description = "是否启用") private Integer enabled;
}
