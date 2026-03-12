package com.mes.process.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 指导书人员 DTO
 */
@Data
@Schema(description = "指导书人员请求参数")
public class WorkInstructionPersonDTO {

    @Schema(description = "人员编号")
    private String personCode;

    @Schema(description = "姓名")
    private String personName;

    @Schema(description = "人员分类")
    private String personCategory;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;
}
