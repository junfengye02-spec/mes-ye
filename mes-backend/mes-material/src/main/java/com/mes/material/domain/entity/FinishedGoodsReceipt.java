package com.mes.material.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 完工入库单主表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_finished_goods_receipt")
public class FinishedGoodsReceipt extends BaseEntity {

    private String receiptNo;
    private String receiptType;
    private String warehouse;
    private String movementType;
    private LocalDateTime planReceiptTime;
    private LocalDateTime actualReceiptTime;
    private String status;
}
