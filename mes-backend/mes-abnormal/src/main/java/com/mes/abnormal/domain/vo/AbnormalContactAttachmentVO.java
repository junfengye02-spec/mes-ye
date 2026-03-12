package com.mes.abnormal.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异常联络单附件返回 VO
 */
@Data
@Schema(description = "异常联络单附件信息")
public class AbnormalContactAttachmentVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "异常联络单ID")
    private Long contactId;

    @Schema(description = "文件编号")
    private String fileNo;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件路径")
    private String fileUrl;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "负责人")
    private String responsiblePerson;

    @Schema(description = "团队")
    private String team;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "法大大标识")
    private String fadadaFlag;

    @Schema(description = "已签")
    private Integer signed;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}
