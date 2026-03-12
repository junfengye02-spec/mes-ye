package com.mes.abnormal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 异常联络单附件表实体
 */
@Data
@TableName("mes_abnormal_contact_attachment")
public class AbnormalContactAttachment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

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

    /** 法大大标识 */
    private String fadadaFlag;

    /** 已签 */
    private Integer signed;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 修改时间 */
    private LocalDateTime updatedTime;
}
