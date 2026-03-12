package com.mes.aps.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作清单任务分段表实体（APS TaskSegment 映射）
 */
@Data
@TableName("mes_work_order_task_segment")
public class WorkOrderTaskSegment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 工作清单项ID */
    private Long workOrderTaskId;
    /** 分段序号 */
    private Integer segmentIndex;
    /** 分段开始时间 */
    private LocalDateTime segmentStartTime;
    /** 分段结束时间 */
    private LocalDateTime segmentEndTime;
    /** 分段时长（分钟） */
    private Integer segmentDuration;
    /** 所属班次名称 */
    private String shiftName;
    /** 负责班组ID */
    private Long assignedTeamId;
    /** 实际开始时间 */
    private LocalDateTime actualStartTime;
    /** 实际结束时间 */
    private LocalDateTime actualEndTime;
    /** 分段状态 */
    private String status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
