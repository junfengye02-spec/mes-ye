package com.mes.aps.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "数据映射信息")
public class ApsDataMappingVO {
    @Schema(description = "ID") private Long id;
    @Schema(description = "映射类型") private String mappingType;
    @Schema(description = "MES编码") private String mesCode;
    @Schema(description = "MES名称") private String mesName;
    @Schema(description = "APS编码") private String apsCode;
    @Schema(description = "APS名称") private String apsName;
    @Schema(description = "是否启用") private Integer enabled;
    @Schema(description = "创建时间") private LocalDateTime createdTime;
    @Schema(description = "更新时间") private LocalDateTime updatedTime;
}
