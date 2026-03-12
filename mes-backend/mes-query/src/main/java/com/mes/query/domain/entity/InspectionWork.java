package com.mes.query.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检验工作表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_inspection_work")
public class InspectionWork extends BaseEntity {

    /** 工作编号 */
    private String workNo;

    /** 工作名称 */
    private String workName;

    /** 计划检验数量 */
    private BigDecimal planInspectQty;

    /** 已检数量 */
    private BigDecimal inspectedQty;

    /** 合格数量 */
    private BigDecimal qualifiedQty;

    /** 不合格数量 */
    private BigDecimal unqualifiedQty;

    /** 判定 */
    private String judgment;

    /** 检验点 */
    private Integer isCheckPoint;

    /** 分派状态 */
    private String dispatchStatus;

    /** 检验中取样 */
    private String samplingInInspect;

    /** 工作状态 */
    private String workStatus;

    /** 检验类 */
    private String inspectType;

    /** 检验类型 */
    private String inspectCategory;

    /** 质检组织 */
    private String qcOrg;

    /** 检验工厂 */
    private String inspectFactory;

    /** 计划班组/检测室 */
    private String planTeamLab;

    /** 实际开始时间 */
    private LocalDateTime actualStartTime;

    /** 实际完成时间 */
    private LocalDateTime actualEndTime;

    /** 报告点 */
    private Integer isReportPoint;

    /** 所属工单ID */
    private Long workOrderId;

    /** 所属工单号 */
    private String workOrderNo;

    /** 工单状态 */
    private String orderStatus;

    /** 说明 */
    private String description;
}
