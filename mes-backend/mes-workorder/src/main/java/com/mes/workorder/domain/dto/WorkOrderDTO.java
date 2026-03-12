package com.mes.workorder.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产工单 DTO
 */
@Data
@Schema(description = "生产工单创建/修改参数")
public class WorkOrderDTO {

    @NotBlank(message = "工单号不能为空")
    @Schema(description = "工单号")
    private String workOrderNo;

    @Schema(description = "工单类型")
    private String workOrderType;

    @Schema(description = "生产计划")
    private String productionPlanNo;

    @Schema(description = "订单计划")
    private String orderPlanNo;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "主产品")
    private String mainProduct;

    @Schema(description = "机型")
    private String machineModel;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "产品类型")
    private String productType;

    @Schema(description = "制造BOM")
    private String bomCode;

    @Schema(description = "项目")
    private String projectName;

    @Schema(description = "WBS元素")
    private String wbsElement;

    @Schema(description = "新制维修类型")
    private String newOrRepairType;

    @Schema(description = "类型")
    private String workType;

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

    @Schema(description = "指定工作中心ID")
    private Long specifiedWorkCenterId;

    @Schema(description = "序列号")
    private String serialNo;

    @Schema(description = "特殊库存标识")
    private String specialStockFlag;

    @Schema(description = "交货地点")
    private String deliveryLocation;

    @Schema(description = "说明")
    private String remark;

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    @Schema(description = "工作清单列表")
    private List<WorkOrderTaskDTO> tasks;

    @Schema(description = "输入物料列表")
    private List<WorkOrderInputMaterialDTO> inputMaterials;

    @Schema(description = "输出物料列表")
    private List<WorkOrderOutputMaterialDTO> outputMaterials;

    @Schema(description = "检验项目列表")
    private List<WorkOrderQualityItemDTO> qualityItems;

    @Schema(description = "约束关系列表")
    private List<WorkOrderConstraintDTO> constraints;

    @Schema(description = "供应计划列表")
    private List<WorkOrderSupplyPlanDTO> supplyPlans;
}
