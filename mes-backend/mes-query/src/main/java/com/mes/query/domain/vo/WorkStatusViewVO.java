package com.mes.query.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "工作状态查看信息")
public class WorkStatusViewVO {
    @Schema(description = "ID") private Long id;
    @Schema(description = "编号") private String workNo;
    @Schema(description = "顺序号") private Integer sequenceNo;
    @Schema(description = "工序号") private String processNo;
    @Schema(description = "名称") private String workName;
    @Schema(description = "是否产出") private Integer isOutput;
    @Schema(description = "工序过程表单") private String processForm;
    @Schema(description = "加工图纸") private String processDrawing;
    @Schema(description = "状态") private String status;
    @Schema(description = "说明") private String description;
    @Schema(description = "资源组编码") private String resourceGroupCode;
    @Schema(description = "所属工序") private String belongProcess;
    @Schema(description = "工厂") private String factory;
    @Schema(description = "业务组织") private String businessOrg;
    @Schema(description = "计划工作中心") private String planWorkCenterName;
    @Schema(description = "指定工作中心") private String specifiedWorkCenterName;
    @Schema(description = "计划班组") private String planTeamName;
    @Schema(description = "计划班次") private String planShift;
    @Schema(description = "来源单号") private String sourceNo;
    @Schema(description = "创建时间") private LocalDateTime createdTime;
    @Schema(description = "计划开始时间") private LocalDateTime planStartTime;
    @Schema(description = "计划结束时间") private LocalDateTime planEndTime;
    @Schema(description = "实际开始时间") private LocalDateTime actualStartTime;
    @Schema(description = "实际完成时间") private LocalDateTime actualEndTime;
    @Schema(description = "下发") private Integer issued;
}
