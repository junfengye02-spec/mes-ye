package com.mes.process.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工序信息查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工序信息查询参数")
public class ProcessInfoQuery extends PageQuery {

    @Schema(description = "工序号")
    private String processNo;

    @Schema(description = "工序名")
    private String processName;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "工序类型")
    private String processType;

    @Schema(description = "工作中心ID")
    private Long workCenterId;
}
