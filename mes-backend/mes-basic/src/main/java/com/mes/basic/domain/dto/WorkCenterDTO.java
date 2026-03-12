package com.mes.basic.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 工作中心新增/编辑 DTO
 */
@Data
@Schema(description = "工作中心请求参数")
public class WorkCenterDTO {

    @NotBlank(message = "工作中心编码不能为空")
    @Schema(description = "工作中心编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workCenterCode;

    @NotBlank(message = "工作中心名称不能为空")
    @Schema(description = "工作中心名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String workCenterName;

    @Schema(description = "工作中心分类")
    private String workCenterCategory;

    @Schema(description = "业务单元")
    private String businessUnit;

    @Schema(description = "工作日历")
    private String workCalendar;

    @Schema(description = "资源排序")
    private Integer resourceOrder;

    @DecimalMin(value = "0", message = "使用量不能为负数")
    @Schema(description = "使用量")
    private BigDecimal usageQty;

    @Schema(description = "使用量单位")
    private String usageUnit;

    @DecimalMin(value = "0", message = "处理批量不能为负数")
    @Schema(description = "处理批量")
    private BigDecimal batchQty;

    @DecimalMin(value = "0", message = "效率不能为负数")
    @Schema(description = "效率")
    private BigDecimal efficiency;

    @Schema(description = "资源种类")
    private String resourceType;

    @Schema(description = "炉资源类型")
    private String furnaceResourceType;

    @DecimalMin(value = "0", message = "资源能力不能为负数")
    @Schema(description = "资源能力")
    private BigDecimal resourceCapacity;

    @Schema(description = "工序不中断")
    private Integer processNoInterrupt;

    @Schema(description = "工序不跨天")
    private Integer processNoCrossDay;

    @Schema(description = "固定节拍点生产")
    private Integer fixedTaktProduction;
}
