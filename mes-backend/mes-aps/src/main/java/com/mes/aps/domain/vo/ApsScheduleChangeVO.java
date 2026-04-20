package com.mes.aps.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "APS排程变更通知VO")
public class ApsScheduleChangeVO {

    @Schema(description = "请求ID")
    private String requestId;

    @Schema(description = "排程批次号")
    private String scheduleBatchId;

    @Schema(description = "变更原因")
    private String changeReason;

    @Schema(description = "变更时间")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime changeTime;

    @Schema(description = "受影响工单列表")
    private List<AffectedOrder> affectedOrders;

    @Data
    @Schema(description = "受影响工单")
    public static class AffectedOrder {

        @Schema(description = "工单号")
        private String workOrderNo;

        @Schema(description = "订单号")
        private String orderNo;

        @Schema(description = "变更类型（TIME_CHANGED/RESOURCE_CHANGED/CANCELLED/NEW）")
        private String changeType;

        @Schema(description = "原计划开始时间")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
        private LocalDateTime oldStartTime;

        @Schema(description = "新计划开始时间")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
        private LocalDateTime newStartTime;

        @Schema(description = "原计划结束时间")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
        private LocalDateTime oldEndTime;

        @Schema(description = "新计划结束时间")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
        private LocalDateTime newEndTime;

        @Schema(description = "变更说明")
        private String remark;
    }
}
