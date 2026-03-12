package com.mes.dispatch.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 派工分配 VO
 */
@Data
@Schema(description = "派工分配视图对象")
public class DispatchAssignmentVO {

    private Long id;
    private Long dispatchTaskId;
    private String assignType;
    private Long assigneeId;
    private String assigneeCode;
    private String assigneeName;
    private BigDecimal assignedQty;
    private String qtyUnit;
    private String status;
    private String assignedBy;
    private LocalDateTime assignedTime;
    private String revokedBy;
    private LocalDateTime revokedTime;
}
