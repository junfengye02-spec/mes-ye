package com.mes.workorder.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "文档附件视图对象")
public class WorkOrderAttachmentVO {

    private Long id;
    private Long workOrderId;
    private String fileName;
    private String fileType;
    private Integer fileSizeKb;
    private String fileUrl;
    private LocalDateTime fileModifiedTime;
    private String modifiedBy;
    private LocalDateTime modifiedTime;
    private String createdBy;
    private LocalDateTime createdTime;
}
