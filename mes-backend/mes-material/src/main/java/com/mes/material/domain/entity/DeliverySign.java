package com.mes.material.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发货签收表实体
 */
@Data
@TableName("mes_delivery_sign")
public class DeliverySign implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
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
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
