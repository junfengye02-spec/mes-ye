package com.mes.query.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产工作表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_production_work")
public class ProductionWork extends BaseEntity {

    /** 工作编号 */
    private String workNo;

    /** 工作名称 */
    private String workName;

    /** 生产工单ID */
    private Long workOrderId;

    /** 生产工单号 */
    private String workOrderNo;

    /** 产品物料 */
    private String productMaterial;

    /** 生产工厂 */
    private String productionFactory;

    /** 生产组织 */
    private String productionOrg;

    /** 实际开始时间 */
    private LocalDateTime actualStartTime;

    /** 实际结束时间 */
    private LocalDateTime actualEndTime;

    /** 计划开始时间 */
    private LocalDateTime planStartTime;

    /** 计划结束时间 */
    private LocalDateTime planEndTime;

    /** 实际处理时间 */
    private BigDecimal actualProcessTime;

    /** 时间单位 */
    private String timeUnit;

    /** 报告点 */
    private Integer isReportPoint;

    /** 检验点 */
    private Integer isCheckPoint;

    /** 交接点 */
    private Integer isHandoverPoint;

    /** 备注 */
    private String remark;
}
