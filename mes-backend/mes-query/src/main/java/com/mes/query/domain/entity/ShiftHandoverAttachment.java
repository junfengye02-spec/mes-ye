package com.mes.query.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 交班记录附件表实体
 */
@Data
@TableName("mes_shift_handover_attachment")
public class ShiftHandoverAttachment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 交班记录ID */
    private Long handoverId;

    /** 文件名 */
    private String fileName;

    /** 文件路径 */
    private String fileUrl;

    /** 文件类型 */
    private String fileType;

    /** 文件大小 */
    private String fileSize;

    /** 上传人 */
    private String uploader;

    /** 下载次数 */
    private Integer downloadCount;

    /** 状态 */
    private String loadStatus;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;
}
