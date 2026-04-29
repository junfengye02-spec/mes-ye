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

    @Schema(description = "实际开工时间")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际完工时间")
    private LocalDateTime actualEndTime;

    @Schema(description = "实际完成数量")
    private BigDecimal actualQty;

    @Schema(description = "质量结果：PASS/FAIL/NA")
    private String qualityResult;

    @Schema(description = "撤销原因")
    private String cancelReason;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "更新人")
    private String updatedBy;

    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    @Schema(description = "分配记录列表")
    private List<DispatchAssignmentVO> assignments;
}
