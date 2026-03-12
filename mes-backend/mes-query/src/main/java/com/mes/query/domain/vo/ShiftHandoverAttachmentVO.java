package com.mes.query.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "交班记录附件信息")
public class ShiftHandoverAttachmentVO {
    @Schema(description = "ID") private Long id;
    @Schema(description = "交班记录ID") private Long handoverId;
    @Schema(description = "文件名") private String fileName;
    @Schema(description = "文件路径") private String fileUrl;
    @Schema(description = "文件类型") private String fileType;
    @Schema(description = "文件大小") private String fileSize;
    @Schema(description = "上传人") private String uploader;
    @Schema(description = "下载次数") private Integer downloadCount;
    @Schema(description = "状态") private String loadStatus;
    @Schema(description = "创建时间") private LocalDateTime createdTime;
}
