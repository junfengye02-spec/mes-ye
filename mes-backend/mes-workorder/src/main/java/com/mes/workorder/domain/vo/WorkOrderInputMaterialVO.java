package com.mes.workorder.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "输入物料视图对象")
public class WorkOrderInputMaterialVO {

    private Long id;
    private Long workOrderId;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private BigDecimal requiredQty;
    private BigDecimal issuedQty;
    private String qtyUnit;
    private String batchNo;
    private String serialNo;
}
