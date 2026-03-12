package com.mes.workorder.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "工作清单视图对象")
public class WorkOrderTaskVO {

    private Long id;
    private Long workOrderId;
    private String taskNo;
    private String taskName;
    private Long planWorkCenterId;
    private BigDecimal planQty;
    private String qtyUnit;
    private String status;
    private Integer sequenceNo;
    private String serialNo;
    private String projectName;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
