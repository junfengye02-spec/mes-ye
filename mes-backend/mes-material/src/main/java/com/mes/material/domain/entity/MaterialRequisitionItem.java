package com.mes.material.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产领料明细表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_material_requisition_item")
public class MaterialRequisitionItem extends BaseEntity {
    private Long requisitionId;
    private Long workOrderId;
    private Long workId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private BigDecimal demandQty;
    private BigDecimal pendingQty;
    private BigDecimal issueQty;
    private String unit;
    private String issueLocation;
    private LocalDateTime demandTime;
    private String description;
    private Integer isFinal;
}
