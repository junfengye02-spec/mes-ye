package com.mes.aps.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 转厂订单表实体（APS TransferOrder 映射）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_transfer_order")
public class TransferOrder extends BaseEntity {

    private String transferNo;
    private String parentOrderNo;
    private Long apsTransferId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private BigDecimal planQty;
    private String fromFactory;
    private String toFactory;
    private LocalDateTime planShipTime;
    private LocalDateTime planArriveTime;
    private String apsStatus;
    private String mesStatus;
    private LocalDateTime actualShipTime;
    private LocalDateTime actualArriveTime;
    private BigDecimal receivedQty;
    private String remark;
}
