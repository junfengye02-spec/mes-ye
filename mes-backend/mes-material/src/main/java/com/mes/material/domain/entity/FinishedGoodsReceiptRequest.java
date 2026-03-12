package com.mes.material.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 完工入库申请表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_finished_goods_receipt_request")
public class FinishedGoodsReceiptRequest extends BaseEntity {

    private String requestNo;
    private String receiptType;
    private Long workOrderId;
    private String workOrderNo;
    private String projectName;
    private String wbsElement;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String serialNo;
    private BigDecimal qty;
    private BigDecimal unqualifiedQty;
    private BigDecimal qualifiedQty;
    private String unit;
    private String description;
    private LocalDateTime planReceiptTime;
    private BigDecimal actualProductionQty;
    private BigDecimal pendingReceiptQty;
    private String status;
}
