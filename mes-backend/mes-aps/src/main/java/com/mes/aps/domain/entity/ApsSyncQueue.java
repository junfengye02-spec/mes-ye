package com.mes.aps.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * APS待同步队列表实体
 */
@Data
@TableName("mes_aps_sync_queue")
public class ApsSyncQueue implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 同步方向 */
    private String syncDirection;
    /** 同步类型 */
    private String syncType;
    /** 数据类型 */
    private String dataType;
    /** 关联数据ID */
    private Long dataId;
    /** 关联数据编号 */
    private String dataNo;
    /** 优先级（1最高，10最低） */
    private Integer priority;
    /** 状态 */
    private String syncStatus;
    /** 已重试次数 */
    private Integer retryCount;
    /** 最大重试次数 */
    private Integer maxRetry;
    /** 下次重试时间 */
    private LocalDateTime nextRetryTime;
    /** 同步数据载荷（JSON） */
    private String payload;
    /** 错误信息 */
    private String errorMessage;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
