package com.mes.aps.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * APS同步日志表实体
 */
@Data
@TableName("mes_aps_sync_log")
public class ApsSyncLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 同步批次号 */
    private String batchId;
    /** 同步方向 */
    private String syncDirection;
    /** 同步类型 */
    private String syncType;
    /** 数据总量 */
    private Integer totalCount;
    /** 成功数量 */
    private Integer successCount;
    /** 失败数量 */
    private Integer failCount;
    /** 同步状态 */
    private String status;
    /** 开始时间 */
    private LocalDateTime startTime;
    /** 结束时间 */
    private LocalDateTime endTime;
    /** 耗时（毫秒） */
    private Long durationMs;
    /** 错误信息 */
    private String errorMessage;
    private LocalDateTime createdTime;
}
