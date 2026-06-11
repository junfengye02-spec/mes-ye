package com.mes.quality.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 复检申请订单计划关联表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_recheck_order_plan")
public class RecheckOrderPlan extends BaseEntity {

    /** 复检申请ID */
    private Long recheckId;

    /** 订单计划ID */
    private Long orderPlanId;

    /** 被关联对象 */
    private String relatedObject;
}
