package com.mes.workorder.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 输入物料表实体
 */
@Data
@TableName("mes_work_order_input_material")
public class WorkOrderInputMaterial implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long workOrderId;

    /** 物料ID */
    private Long materialId;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 需求数量 */
    private BigDecimal requiredQty;

    /** 已发数量 */
    private BigDecimal issuedQty;

    /** 数量单位 */
    private String qtyUnit;

    /** 批号 */
    private String batchNo;

    /** 序列号 */
    private String serialNo;
}
