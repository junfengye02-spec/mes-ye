package com.mes.aps.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "APS资源分配计划回调VO")
public class ApsResourceAllocationVO {

    @Schema(description = "请求ID")
    private String requestId;

    @Schema(description = "排程批次号")
    private String scheduleBatchId;

    @Schema(description = "资源分配明细")
    private List<AllocationItem> items;

    @Data
    @Schema(description = "资源分配明细")
    public static class AllocationItem {

        @Schema(description = "工单号")
        private String workOrderNo;

        @Schema(description = "工序号")
        private String processNo;

        @Schema(description = "工作中心编码")
        private String workCenterCode;

        @Schema(description = "分配类型（DEVICE/PERSON/TEAM）")
        private String assignType;

        @Schema(description = "分配对象编码")
        private String assigneeCode;

        @Schema(description = "分配对象名称")
        private String assigneeName;

        @Schema(description = "分配数量")
        private BigDecimal assignedQty;

        @Schema(description = "计划开始时间")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
        private LocalDateTime planStartTime;

        @Schema(description = "计划结束时间")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
        private LocalDateTime planEndTime;
    }
}
