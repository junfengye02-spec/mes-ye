package com.mes.dispatch.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 派工任务 VO
 */
@Data
@Schema(description = "派工任务视图对象")
public class DispatchTaskVO {

    private Long id;
    private Long workOrderId;
    private Long workOrderTaskId;
    private String orderNo;
    private String processNo;
    private String workName;
    private Long planWorkCenterId;
    private String serialNo;
    private String projectName;
    private BigDecimal planQty;
    private String qtyUnit;
    private String dispatchStatus;
    private LocalDateTime planStartTime;
    private LocalDateTime planEndTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    @Schema(description = "分配记录列表")
    private List<DispatchAssignmentVO> assignments;
}
