package com.mes.workorder.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 约束关系表实体
 */
@Data
@TableName("mes_work_order_constraint")
public class WorkOrderConstraint implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long workOrderId;

    /** 约束类型 */
    private String constraintType;

    /** 关联工单ID */
    private Long relatedWorkOrderId;

    /** 关联工作清单ID */
    private Long relatedTaskId;

    /** 说明 */
    private String remark;
}
