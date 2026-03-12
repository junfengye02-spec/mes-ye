package com.mes.workorder.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 检验项目清单表实体
 */
@Data
@TableName("mes_work_order_quality_item")
public class WorkOrderQualityItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long workOrderId;

    /** 检验项目编号 */
    private String qualityItemCode;

    /** 检验项目名称 */
    private String qualityItemName;

    /** 检验要求 */
    private String requirement;

    /** 状态 */
    private String status;
}
