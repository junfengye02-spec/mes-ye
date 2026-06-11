package com.mes.aps.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * APS 甘特图缓存实体
 */
@Data
@TableName("mes_aps_gantt_cache")
public class ApsGanttCache implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 排程批次号 */
    private String scheduleBatchId;
    /** APS 任务ID */
    private String taskId;
    /** 工单号 */
    private String workOrderNo;
    /** 订单号 */
    private String orderNo;
    /** 产品编码 */
    private String productCode;
    /** 产品名称 */
    private String productName;
    /** 工序号 */
    private String processNo;
    /** 工序名称 */
    private String processName;
    /** 资源编码 */
    private String resourceCode;
    /** 资源名称 */
    private String resourceName;
    /** 计划开始时间 */
    private LocalDateTime startTime;
    /** 计划结束时间 */
    private LocalDateTime endTime;
    /** 时长（分钟） */
    private Integer duration;
    /** 状态 */
    private String status;
    /** 优先级 */
    private Integer priority;
    /** 前置任务 ID JSON */
    private String predecessors;
    /** 排程范围开始 */
    private LocalDateTime rangeStart;
    /** 排程范围结束 */
    private LocalDateTime rangeEnd;
    private LocalDateTime createdTime;
}
