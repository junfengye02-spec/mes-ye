package com.mes.team.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生产班组查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "生产班组查询参数")
public class ProductionTeamQuery extends PageQuery {

    @Schema(description = "班组编码")
    private String teamCode;

    @Schema(description = "班组名称")
    private String teamName;

    @Schema(description = "是否启用（1=启用, 0=停用, null=全部）")
    private Integer enabled;
}
