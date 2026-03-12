package com.mes.process.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 指示书查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "指示书查询参数")
public class InstructionQuery extends PageQuery {

    @Schema(description = "指示书号")
    private String instructionNo;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "产品类型")
    private String productType;
}
