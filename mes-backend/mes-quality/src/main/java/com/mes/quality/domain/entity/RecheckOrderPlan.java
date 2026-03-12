package com.mes.quality.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 复检申请订单计划关联表实体
 */
@Data
@TableName("mes_recheck_order_plan")
public class RecheckOrderPlan implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 复检申请ID */
    private Long recheckId;

    /** 订单计划ID */
    private Long orderPlanId;

    /** 被关联对象 */
    private String relatedObject;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 修改人 */
    private String updatedBy;

    /** 修改时间 */
    private LocalDateTime updatedTime;
}
