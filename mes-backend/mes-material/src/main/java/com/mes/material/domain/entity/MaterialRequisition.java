package com.mes.material.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产领料申请表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_material_requisition")
public class MaterialRequisition extends BaseEntity {

    private String requisitionNo;
    private Long workOrderId;
    private String workOrderNo;
    private String productCode;
    private String productName;
    private BigDecimal planQty;
    private BigDecimal actualQty;
    private BigDecimal qualifiedQty;
    private String qtyUnit;
    private String mainOrg;
    private LocalDateTime planStartTime;
    private LocalDateTime planEndTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private String salesOrderLine;
    private String projectName;
    private String wbsElement;
    private String status;
}
