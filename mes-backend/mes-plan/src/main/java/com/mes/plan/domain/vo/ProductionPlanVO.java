package com.mes.plan.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产计划 VO
 */
@Data
@Schema(description = "生产计划视图对象")
public class ProductionPlanVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "订单计划ID")
    private Long orderPlanId;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "新制维修类型")
    private String newOrRepairType;

    @Schema(description = "类型")
    private String workType;

    @Schema(description = "机型")
    private String machineModel;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "产品类型")
    private String productType;

    @Schema(description = "WBS元素")
    private String wbsElement;

    @Schema(description = "计划工单类型")
    private String workOrderType;

    @Schema(description = "计划组织")
    private String planOrg;

    @Schema(description = "计划数量")
    private BigDecimal planQty;

    @Schema(description = "数量单位")
    private String qtyUnit;

    @Schema(description = "完工数量")
    private BigDecimal completedQty;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划完成时间")
    private LocalDateTime planEndTime;

    @Schema(description = "实际开始时间")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际完成时间")
    private LocalDateTime actualEndTime;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "修改人")
    private String updatedBy;

    @Schema(description = "修改时间")
    private LocalDateTime updatedTime;
}
