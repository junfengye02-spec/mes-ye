package com.mes.abnormal.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 异常联络单附件表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_abnormal_contact_attachment")
public class AbnormalContactAttachment extends BaseEntity {

    /** 异常联络单ID */
    private Long contactId;

    /** 文件编号 */
    private String fileNo;

    /** 文件名 */
    private String fileName;

    /** 文件路径 */
    private String fileUrl;

    /** 文件类型 */
    private String fileType;

    /** 负责人 */
    private String responsiblePerson;

    /** 团队 */
    private String team;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 签章供应商 */
    private String signatureProvider;

    /** 签章状态 */
    private String signatureStatus;

    /** 法大大标识 */
    private String fadadaFlag;

    /** 已签 */
    private Integer signed;
}
