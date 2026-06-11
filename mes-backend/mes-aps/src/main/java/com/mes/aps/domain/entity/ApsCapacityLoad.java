package com.mes.aps.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * APS 产能负荷缓存实体
 */
@Data
@TableName("mes_aps_capacity_load")
public class ApsCapacityLoad implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 排程批次号 */
    private String scheduleBatchId;
    /** 工作中心编码 */
    private String workCenterCode;
    /** 工作中心名称 */
    private String workCenterName;
    /** 负荷日期 */
    private LocalDate loadDate;
    /** 可用产能（分钟） */
    private BigDecimal availableCapacity;
    /** 已排产能（分钟） */
    private BigDecimal scheduledCapacity;
    /** 负荷率 */
    private BigDecimal loadRate;
    /** 是否超负荷 */
    private Boolean overloaded;
    /** 计算时间 */
    private LocalDateTime calculatedAt;
    private LocalDateTime createdTime;
}
