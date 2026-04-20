package com.mes.aps.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "APS物料需求计划回调VO")
public class ApsMrpCallbackVO {

    @Schema(description = "请求ID")
    private String requestId;

    @Schema(description = "排程批次号")
    private String scheduleBatchId;

    @Schema(description = "物料需求明细列表")
    private List<MrpItem> items;

    @Data
    @Schema(description = "物料需求明细")
    public static class MrpItem {

        @Schema(description = "工单号")
        private String workOrderNo;

        @Schema(description = "工序号")
        private String processNo;

        @Schema(description = "物料编码")
        private String materialCode;

        @Schema(description = "物料名称")
        private String materialName;

        @Schema(description = "需求数量")
        private BigDecimal requiredQty;

        @Schema(description = "计量单位")
        private String unit;

        @Schema(description = "需求日期")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
        private LocalDateTime requiredDate;

        @Schema(description = "优先级")
        private Integer priority;
    }
}
