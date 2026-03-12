package com.mes.quality.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 交班记录查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "交班记录查询参数")
public class ShiftHandoverQuery extends PageQuery {

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "交班日期")
    private LocalDate handoverDate;

    @Schema(description = "发起班组")
    private String handoverTeamName;

    @Schema(description = "状态")
    private String status;
}
