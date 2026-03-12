package com.mes.aps.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "同步配置查询参数")
public class ApsSyncConfigQuery extends PageQuery {
    @Schema(description = "配置键（模糊查询）") private String configKey;
    @Schema(description = "是否启用") private Integer enabled;
}
