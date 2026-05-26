package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工艺路线步骤实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_route_step")
public class RouteStep extends BaseEntity {

    /** 路线ID */
    private Long routeId;

    /** 顺序号 */
    private Integer sequenceNo;

    /** 工序ID */
    private Long processId;

    /** 工序号 */
    private String processNo;

    /** 工序名称 */
    private String processName;

    /** 工作中心ID */
    private Long workCenterId;

    /** 标准处理时间 */
    private BigDecimal handleTime;

    /** 前置步骤ID */
    private Long predecessorStepId;

    /** 是否并行 */
    private Integer parallelFlag;

    /** 是否可选 */
    private Integer optionalFlag;

    /** 备注 */
    private String remark;
}
