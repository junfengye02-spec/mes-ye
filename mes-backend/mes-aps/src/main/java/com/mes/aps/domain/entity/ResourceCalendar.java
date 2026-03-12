package com.mes.aps.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资源日历表实体（APS ResourceCalendar 映射）
 */
@Data
@TableName("mes_resource_calendar")
public class ResourceCalendar implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** APS日历ID */
    private Long apsCalendarId;
    /** 日历名称 */
    private String calendarName;
    /** 关联工作中心ID */
    private Long resourceId;
    /** 关联工作中心编码 */
    private String resourceCode;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
