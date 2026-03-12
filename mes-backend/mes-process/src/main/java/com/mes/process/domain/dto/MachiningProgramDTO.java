package com.mes.process.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 机械加工程序新增/编辑 DTO
 */
@Data
@Schema(description = "机械加工程序请求参数")
public class MachiningProgramDTO {

    @NotBlank(message = "G-code不能为空")
    @Schema(description = "G-code", requiredMode = Schema.RequiredMode.REQUIRED)
    private String gCode;

    @Schema(description = "程序表")
    private String programTable;

    @Schema(description = "产品名称")
    private String productName;
}
