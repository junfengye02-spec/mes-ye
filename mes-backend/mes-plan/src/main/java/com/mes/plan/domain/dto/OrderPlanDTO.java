package com.mes.plan.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单计划 DTO
 */
@Data
@Schema(description = "订单计划创建/修改参数")
public class OrderPlanDTO {

    @NotBlank(message = "订单号不能为空")
    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "项目")
    private String projectName;

    @Schema(description = "WBS元素")
    private String wbsElement;

    @Schema(description = "新制维修类型")
    private String newOrRepairType;

    @Schema(description = "类型（维修/检查/主机）")
    private String workType;

    @Schema(description = "机型")
    private String machineModel;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "产品类型")
    private String productType;

    @DecimalMin(value = "0", message = "计划数量不能小于0")
    @Schema(description = "计划数量")
    private BigDecimal planQty;

    @Schema(description = "数量单位")
    private String qtyUnit;

    @Schema(description = "工厂组织")
    private String factoryOrg;

    @Schema(description = "计划组织")
    private String planOrg;

    @Schema(description = "主制组织")
    private String mainOrg;

    @Schema(description = "计划工作中心ID")
    private Long planWorkCenterId;

    @Schema(description = "是否订单")
    private Boolean isOrder;

    @Schema(description = "PCCL流程")
    private String pcclFlow;

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    @Schema(description = "数据来源（MANUAL/APS）")
    private String dataSource;
}
