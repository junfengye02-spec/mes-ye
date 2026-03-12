package com.mes.workorder.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "文档附件参数")
public class WorkOrderAttachmentDTO {

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "大小(K)")
    private Integer fileSizeKb;

    @Schema(description = "文件路径")
    private String fileUrl;

    @Schema(description = "文件修改时间")
    private LocalDateTime fileModifiedTime;
}
