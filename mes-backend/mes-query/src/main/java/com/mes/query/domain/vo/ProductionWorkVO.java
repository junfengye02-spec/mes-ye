package com.mes.query.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "生产工作信息")
public class ProductionWorkVO {
    @Schema(description = "ID") private Long id;
    @Schema(description = "工作编号") private String workNo;
    @Schema(description = "工作名称") private String workName;
    @Schema(description = "工单ID") private Long workOrderId;
    @Schema(description = "工单号") private String workOrderNo;
    @Schema(description = "产品物料") private String productMaterial;
    @Schema(description = "生产工厂") private String productionFactory;
    @Schema(description = "生产组织") private String productionOrg;
    @Schema(description = "实际开始时间") private LocalDateTime actualStartTime;
    @Schema(description = "实际结束时间") private LocalDateTime actualEndTime;
    @Schema(description = "计划开始时间") private LocalDateTime planStartTime;
    @Schema(description = "计划结束时间") private LocalDateTime planEndTime;
    @Schema(description = "实际处理时间") private BigDecimal actualProcessTime;
    @Schema(description = "时间单位") private String timeUnit;
    @Schema(description = "报告点") private Integer isReportPoint;
    @Schema(description = "检验点") private Integer isCheckPoint;
    @Schema(description = "交接点") private Integer isHandoverPoint;
    @Schema(description = "备注") private String remark;
    @Schema(description = "创建时间") private LocalDateTime createdTime;
}
