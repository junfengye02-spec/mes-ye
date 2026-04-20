package com.mes.aps.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "APS产能负荷数据VO")
public class ApsCapacityLoadVO {

    @Schema(description = "请求ID")
    private String requestId;

    @Schema(description = "排程批次号")
    private String scheduleBatchId;

    @Schema(description = "统计时间")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime calculatedAt;

    @Schema(description = "产能负荷明细")
    private List<CapacityItem> items;

    @Data
    @Schema(description = "产能负荷明细")
    public static class CapacityItem {

        @Schema(description = "工作中心编码")
        private String workCenterCode;

        @Schema(description = "工作中心名称")
        private String workCenterName;

        @Schema(description = "日期")
        private LocalDate date;

        @Schema(description = "可用产能（分钟）")
        private BigDecimal availableCapacity;

        @Schema(description = "已排产能（分钟）")
        private BigDecimal scheduledCapacity;

        @Schema(description = "负荷率（%）")
        private BigDecimal loadRate;

        @Schema(description = "是否超负荷")
        private Boolean overloaded;
    }
}
