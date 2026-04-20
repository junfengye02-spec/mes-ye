package com.mes.aps.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "APS排程甘特图数据VO")
public class ApsGanttDataVO {

    @Schema(description = "请求ID")
    private String requestId;

    @Schema(description = "排程批次号")
    private String scheduleBatchId;

    @Schema(description = "排程时间范围-开始")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime rangeStart;

    @Schema(description = "排程时间范围-结束")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime rangeEnd;

    @Schema(description = "甘特图任务列表")
    private List<GanttTask> tasks;

    @Data
    @Schema(description = "甘特图任务")
    public static class GanttTask {

        @Schema(description = "任务ID")
        private String taskId;

        @Schema(description = "工单号")
        private String workOrderNo;

        @Schema(description = "订单号")
        private String orderNo;

        @Schema(description = "产品编码")
        private String productCode;

        @Schema(description = "产品名称")
        private String productName;

        @Schema(description = "工序号")
        private String processNo;

        @Schema(description = "工序名称")
        private String processName;

        @Schema(description = "资源编码（工作中心）")
        private String resourceCode;

        @Schema(description = "资源名称")
        private String resourceName;

        @Schema(description = "计划开始时间")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
        private LocalDateTime startTime;

        @Schema(description = "计划结束时间")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
        private LocalDateTime endTime;

        @Schema(description = "时长（分钟）")
        private Integer duration;

        @Schema(description = "状态")
        private String status;

        @Schema(description = "优先级")
        private Integer priority;

        @Schema(description = "前置任务ID列表")
        private List<String> predecessors;
    }
}
