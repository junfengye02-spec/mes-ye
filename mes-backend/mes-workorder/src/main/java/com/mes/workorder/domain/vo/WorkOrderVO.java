package com.mes.workorder.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产工单 VO
 */
@Data
@Schema(description = "生产工单视图对象")
public class WorkOrderVO {

    @Schema(description = "ID")
    private Long id;

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

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务类型（兼容字段）")
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

    @Schema(description = "状态")
    private String status;

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    @Schema(description = "实际开始时间")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间")
    private LocalDateTime actualEndTime;

    @Schema(description = "序列号")
    private String serialNo;

    @Schema(description = "特殊库存标识")
    private String specialStockFlag;

    @Schema(description = "交货地点")
    private String deliveryLocation;

    @Schema(description = "说明")
    private String remark;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "修改人")
    private String updatedBy;

    @Schema(description = "修改时间")
    private LocalDateTime updatedTime;

    // 子表数据（详情查询时返回）
    @Schema(description = "工作清单")
    private List<WorkOrderTaskVO> tasks;

    @Schema(description = "输入物料")
    private List<WorkOrderInputMaterialVO> inputMaterials;

    @Schema(description = "输出物料")
    private List<WorkOrderOutputMaterialVO> outputMaterials;

    @Schema(description = "检验项目")
    private List<WorkOrderQualityItemVO> qualityItems;

    @Schema(description = "约束关系")
    private List<WorkOrderConstraintVO> constraints;

    @Schema(description = "供应计划")
    private List<WorkOrderSupplyPlanVO> supplyPlans;

    @Schema(description = "文档附件")
    private List<WorkOrderAttachmentVO> attachments;
}
