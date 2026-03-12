package com.mes.workorder.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档附件表实体
 */
@Data
@TableName("mes_work_order_attachment")
public class WorkOrderAttachment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long workOrderId;

    /** 文件名 */
    private String fileName;

    /** 文件类型 */
    private String fileType;

    /** 大小(K) */
    private Integer fileSizeKb;

    /** 文件路径 */
    private String fileUrl;

    /** 文件修改时间 */
    private LocalDateTime fileModifiedTime;

    /** 修改人 */
    private String modifiedBy;

    /** 修改时间 */
    private LocalDateTime modifiedTime;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;
}
