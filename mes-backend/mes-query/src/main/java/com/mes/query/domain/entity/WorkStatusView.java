package com.mes.query.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作状态查看表实体
 */
@Data
@TableName("mes_work_status_view")
public class WorkStatusView implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 编号 */
    private String workNo;

    /** 顺序号 */
    private Integer sequenceNo;

    /** 工序号 */
    private String processNo;

    /** 名称 */
    private String workName;

    /** 是否产出 */
    private Integer isOutput;

    /** 工序过程表单 */
    private String processForm;

    /** 加工图纸 */
    private String processDrawing;

    /** 状态（CREATED/RELEASED/ISSUED/IN_PROGRESS/COMPLETED/PAUSED） */
    private String status;

    /** 说明 */
    private String description;

    /** 组炉号 */
    private String furnaceNo;

    /** 所属工序 */
    private String belongProcess;

    /** 工厂 */
    private String factory;

    /** 业务组织 */
    private String businessOrg;

    /** 计划工段 */
    private String planSection;

    /** 计划工作中心ID */
    private Long planWorkCenterId;

    /** 计划工作中心 */
    private String planWorkCenterName;

    /** 指定工段 */
    private String specifiedSection;

    /** 指定工作中心ID */
    private Long specifiedWorkCenterId;

    /** 指定工作中心 */
    private String specifiedWorkCenterName;

    /** 计划班组ID */
    private Long planTeamId;

    /** 计划班组 */
    private String planTeamName;

    /** 计划班次 */
    private String planShift;

    /** 来源单号 */
    private String sourceNo;

    /** 时间单位 */
    private String timeUnit;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 计划开始时间 */
    private LocalDateTime planStartTime;

    /** 计划结束时间 */
    private LocalDateTime planEndTime;

    /** 实际开始时间 */
    private LocalDateTime actualStartTime;

    /** 实际完成时间 */
    private LocalDateTime actualEndTime;

    /** 审批备注 */
    private String approvalRemark;

    /** 下发 */
    private Integer issued;
}
