package com.mes.process.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 喷涂条件查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "喷涂条件查询参数")
public class SprayConditionQuery extends PageQuery {

    @Schema(description = "条件号")
    private String conditionNo;

    @Schema(description = "喷枪型号")
    private String sprayGunModel;

    @Schema(description = "设备")
    private String equipment;
}
