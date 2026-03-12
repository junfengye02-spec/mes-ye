package com.mes.team.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 生产班组新增/编辑 DTO
 */
@Data
@Schema(description = "生产班组请求参数")
public class ProductionTeamDTO {

    @NotBlank(message = "班组编码不能为空")
    @Schema(description = "班组编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String teamCode;

    @NotBlank(message = "班组名称不能为空")
    @Schema(description = "班组名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String teamName;

    @Schema(description = "生产组织ID")
    private Long orgId;

    @Schema(description = "生产组织编码")
    private String orgCode;

    @Schema(description = "生产组织名称")
    private String orgName;

    @Schema(description = "说明")
    private String description;
}
