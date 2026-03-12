package com.mes.basic.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作中心查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工作中心查询参数")
public class WorkCenterQuery extends PageQuery {

    @Schema(description = "工作中心编码")
    private String workCenterCode;

    @Schema(description = "工作中心名称")
    private String workCenterName;

    @Schema(description = "工作中心分类")
    private String workCenterCategory;

    @Schema(description = "业务单元")
    private String businessUnit;
}
