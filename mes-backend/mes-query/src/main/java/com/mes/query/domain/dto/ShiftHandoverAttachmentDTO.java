package com.mes.query.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "交班记录附件参数")
public class ShiftHandoverAttachmentDTO {

    @NotNull(message = "交班记录ID不能为空")
    @Schema(description = "交班记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long handoverId;

    @NotBlank(message = "文件名不能为空")
    @Schema(description = "文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;

    @NotBlank(message = "文件路径不能为空")
    @Schema(description = "文件路径", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileUrl;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件大小")
    private String fileSize;
}
