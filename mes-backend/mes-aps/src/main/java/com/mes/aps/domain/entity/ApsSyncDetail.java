package com.mes.aps.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * APS同步数据明细表实体
 */
@Data
@TableName("mes_aps_sync_detail")
public class ApsSyncDetail implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 同步批次号 */
    private String batchId;
    /** 数据类型 */
    private String dataType;
    /** 关联数据ID */
    private Long dataId;
    /** 关联数据编号 */
    private String dataNo;
    /** 同步动作 */
    private String syncAction;
    /** 同步状态 */
    private String syncStatus;
    /** APS侧数据快照（JSON） */
    private String apsData;
    /** MES侧数据快照（JSON） */
    private String mesData;
    /** 错误信息 */
    private String errorMessage;
    /** 重试次数 */
    private Integer retryCount;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
