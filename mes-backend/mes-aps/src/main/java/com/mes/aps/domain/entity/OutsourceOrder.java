package com.mes.aps.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 外协订单表实体（APS OutsourceOrder 映射）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_outsource_order")
public class OutsourceOrder extends BaseEntity {

    private String outsourceOrderNo;
    private String parentOrderNo;
    private Long apsOrderId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private BigDecimal planQty;
    private Long supplierId;
    private String supplierName;
    private Integer processSequence;
    private String processName;
    private LocalDateTime planStartTime;
    private LocalDateTime planEndTime;
    private String apsStatus;
    private String mesStatus;
    private LocalDateTime actualShipTime;
    private LocalDateTime actualReceiveTime;
    private BigDecimal receivedQty;
    private BigDecimal qualifiedQty;
    private String remark;
}
