package com.mes.material.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发货签收表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_delivery_sign")
public class DeliverySign extends BaseEntity {
    private String lineNo;
    private Long workOrderId;
    private String workOrderNo;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private BigDecimal planDeliveryQty;
    private BigDecimal pendingSignQty;
    private String unit;
    private String deliveryWarehouse;
    private String deliveryLocation;
    private String orderCreator;
    private LocalDateTime orderCreateTime;
    private String deliverer;
    private LocalDateTime deliveryTime;
}
