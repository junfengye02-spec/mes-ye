package com.mes.query.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "检验工作信息")
public class InspectionWorkVO {
    @Schema(description = "ID") private Long id;
    @Schema(description = "工作编号") private String workNo;
    @Schema(description = "工作名称") private String workName;
    @Schema(description = "计划检验数量") private BigDecimal planInspectQty;
    @Schema(description = "已检数量") private BigDecimal inspectedQty;
    @Schema(description = "合格数量") private BigDecimal qualifiedQty;
    @Schema(description = "不合格数量") private BigDecimal unqualifiedQty;
    @Schema(description = "判定") private String judgment;
    @Schema(description = "检验点") private Integer isCheckPoint;
    @Schema(description = "分派状态") private String dispatchStatus;
    @Schema(description = "工作状态") private String workStatus;
    @Schema(description = "检验类") private String inspectType;
    @Schema(description = "检验类型") private String inspectCategory;
    @Schema(description = "质检组织") private String qcOrg;
    @Schema(description = "检验工厂") private String inspectFactory;
    @Schema(description = "计划班组/检测室") private String planTeamLab;
    @Schema(description = "实际开始时间") private LocalDateTime actualStartTime;
    @Schema(description = "实际完成时间") private LocalDateTime actualEndTime;
    @Schema(description = "报告点") private Integer isReportPoint;
    @Schema(description = "工单ID") private Long workOrderId;
    @Schema(description = "工单号") private String workOrderNo;
    @Schema(description = "工单状态") private String orderStatus;
    @Schema(description = "说明") private String description;
    @Schema(description = "创建时间") private LocalDateTime createdTime;
}
