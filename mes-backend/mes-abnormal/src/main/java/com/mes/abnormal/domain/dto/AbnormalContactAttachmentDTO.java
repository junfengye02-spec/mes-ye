package com.mes.abnormal.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 异常联络单附件 DTO
 */
@Data
@Schema(description = "异常联络单附件参数")
public class AbnormalContactAttachmentDTO {

    @NotBlank(message = "文件名不能为空")
    @Schema(description = "文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;

    @NotBlank(message = "文件路径不能为空")
    @Schema(description = "文件路径", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileUrl;

    @Schema(description = "文件编号")
    private String fileNo;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "负责人")
    private String responsiblePerson;

    @Schema(description = "团队")
    private String team;

    @Schema(description = "签章供应商")
    private String signatureProvider;
}
