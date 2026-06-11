package com.mes.material.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 完工入库明细表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_finished_goods_receipt_item")
public class FinishedGoodsReceiptItem extends BaseEntity {
    private Long receiptId;
    private String itemCode;
    private Long workOrderId;
    private String workOrderNo;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private BigDecimal receiptQty;
    private String unit;
    private String storageLocation;
    private String stagingArea;
    private String stagingBin;
    private String stockStatus;
    private String specialStock;
    private String customer;
    private String wbsElement;
    private String salesOrderLine;
    private BigDecimal varianceQty;
    private String varianceReason;
}
