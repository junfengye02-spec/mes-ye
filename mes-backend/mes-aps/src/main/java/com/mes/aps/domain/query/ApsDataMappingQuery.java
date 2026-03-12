package com.mes.aps.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "数据映射查询参数")
public class ApsDataMappingQuery extends PageQuery {
    @Schema(description = "映射类型") private String mappingType;
    @Schema(description = "MES编码（模糊）") private String mesCode;
    @Schema(description = "APS编码（模糊）") private String apsCode;
    @Schema(description = "是否启用") private Integer enabled;
}
