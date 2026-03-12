package com.mes.process.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机械加工程序查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "机械加工程序查询参数")
public class MachiningProgramQuery extends PageQuery {

    @Schema(description = "G-code")
    private String gCode;

    @Schema(description = "产品名称")
    private String productName;
}
