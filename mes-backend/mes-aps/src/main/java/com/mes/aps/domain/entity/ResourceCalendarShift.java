package com.mes.aps.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 资源日历班次表实体（APS CalendarShift 映射）
 */
@Data
@TableName("mes_resource_calendar_shift")
public class ResourceCalendarShift implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 资源日历ID */
    private Long calendarId;
    /** 星期几 */
    private String dayOfWeek;
    /** 班次开始时间 */
    private LocalTime shiftStartTime;
    /** 班次结束时间 */
    private LocalTime shiftEndTime;
    /** 班次名称 */
    private String shiftName;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
