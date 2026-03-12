package com.mes.quality.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 交班记录 VO
 */
@Data
@Schema(description = "交班记录信息")
public class ShiftHandoverVO {

    @Schema(description = "ID")
    private Long id;
    @Schema(description = "项目名称")
    private String projectName;
    @Schema(description = "产品序列号")
    private String productSerialNo;
    @Schema(description = "工序内容")
    private String processContent;
    @Schema(description = "交班日期")
    private LocalDate handoverDate;
    @Schema(description = "发起星期")
    private Integer handoverWeekday;
    @Schema(description = "发起时间")
    private LocalTime handoverTime;
    @Schema(description = "发起班组ID")
    private Long handoverTeamId;
    @Schema(description = "发起班组")
    private String handoverTeamName;
    @Schema(description = "发起班次")
    private String handoverShift;
    @Schema(description = "接收班次")
    private String takeoverShift;
    @Schema(description = "接班班组ID")
    private Long takeoverTeamId;
    @Schema(description = "接班班组")
    private String takeoverTeamName;
    @Schema(description = "交接人员")
    private String handoverPerson;
    @Schema(description = "接班人员")
    private String takeoverPerson;
    @Schema(description = "班组长")
    private String teamLeader;
    @Schema(description = "计划数量")
    private BigDecimal planQty;
    @Schema(description = "实际完成")
    private BigDecimal actualQty;
    @Schema(description = "未达标分析")
    private String gapAnalysis;
    @Schema(description = "交班内容")
    private String handoverContent;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "其它需要交付事宜")
    private String otherMatters;
    @Schema(description = "创建人")
    private String createdBy;
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}
