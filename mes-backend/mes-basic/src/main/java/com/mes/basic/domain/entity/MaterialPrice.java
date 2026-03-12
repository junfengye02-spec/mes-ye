package com.mes.basic.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 物料价格实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_material_price")
public class MaterialPrice extends BaseEntity {

    /** 物料ID */
    private Long materialId;

    /** 物料单价 */
    private BigDecimal unitPrice;

    /** 单位 */
    private String unit;
}
