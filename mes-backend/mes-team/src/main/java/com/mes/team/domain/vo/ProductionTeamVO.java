package com.mes.team.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 生产班组返回 VO
 */
@Data
@Schema(description = "生产班组信息")
public class ProductionTeamVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "班组编码")
    private String teamCode;

    @Schema(description = "班组名称")
    private String teamName;

    @Schema(description = "生产组织ID")
    private Long orgId;

    @Schema(description = "生产组织编码")
    private String orgCode;

    @Schema(description = "生产组织名称")
    private String orgName;

    @Schema(description = "是否启用")
    private Integer enabled;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "修改人")
    private String updatedBy;

    @Schema(description = "修改时间")
    private LocalDateTime updatedTime;
}
