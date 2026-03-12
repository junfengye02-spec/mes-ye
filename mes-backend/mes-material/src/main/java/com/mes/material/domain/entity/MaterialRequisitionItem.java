package com.mes.material.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产领料明细表实体
 */
@Data
@TableName("mes_material_requisition_item")
public class MaterialRequisitionItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
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
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
