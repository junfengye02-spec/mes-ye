package com.mes.basic.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工作中心返回 VO
 */
@Data
@Schema(description = "工作中心信息")
public class WorkCenterVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "工作中心编码")
    private String workCenterCode;

    @Schema(description = "工作中心名称")
    private String workCenterName;

    @Schema(description = "工作中心分类")
    private String workCenterCategory;

    @Schema(description = "业务单元")
    private String businessUnit;

    @Schema(description = "工作日历")
    private String workCalendar;

    @Schema(description = "资源排序")
    private Integer resourceOrder;

    @Schema(description = "使用量")
    private BigDecimal usageQty;

    @Schema(description = "使用量单位")
    private String usageUnit;

    @Schema(description = "处理批量")
    private BigDecimal batchQty;

    @Schema(description = "效率")
    private BigDecimal efficiency;

    @Schema(description = "资源种类")
    private String resourceType;

    @Schema(description = "炉资源类型")
    private String furnaceResourceType;

    @Schema(description = "资源能力")
    private BigDecimal resourceCapacity;

    @Schema(description = "工序不中断")
    private Integer processNoInterrupt;

    @Schema(description = "工序不跨天")
    private Integer processNoCrossDay;

    @Schema(description = "固定节拍点生产")
    private Integer fixedTaktProduction;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "修改人")
    private String updatedBy;

    @Schema(description = "修改时间")
    private LocalDateTime updatedTime;
}
