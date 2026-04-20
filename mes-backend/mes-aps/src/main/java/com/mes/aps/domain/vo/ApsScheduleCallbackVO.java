package com.mes.aps.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * APS 排程回调数据（与 APS 端 ApsScheduleCallbackDTO 对应）
 */
@Data
@Schema(description = "APS排程回调VO")
public class ApsScheduleCallbackVO {

    @Schema(description = "请求ID")
    private String requestId;

    @Schema(description = "状态: SUCCESS / FAILED / REJECTED")
    private String status;

    @Schema(description = "消息")
    private String message;

    @Schema(description = "拒绝/失败原因")
    private String reason;

    @Schema(description = "排程时间戳")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime scheduledAt;

    @Schema(description = "任务摘要列表")
    private List<TaskSummary> taskSummaries;

    @Data
    @Schema(description = "任务摘要")
    public static class TaskSummary {

        @Schema(description = "APS订单ID")
        private Long apsOrderId;

        @Schema(description = "APS任务ID")
        private Long taskId;

        @Schema(description = "订单编号")
        private String orderNo;

        @Schema(description = "工序名称")
        private String processName;

        @Schema(description = "资源编码")
        private String resourceCode;

        @Schema(description = "计划开始时间")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
        private LocalDateTime startTime;

        @Schema(description = "计划结束时间")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
        private LocalDateTime endTime;

        @Schema(description = "时长（分钟）")
        private Integer duration;

        @Schema(description = "分段序号")
        private Integer segmentIndex;
    }
}
