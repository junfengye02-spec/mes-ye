package com.mes.plan.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单计划 VO
 */
@Data
@Schema(description = "订单计划视图对象")
public class OrderPlanVO {

    @Schema(description = "ID")
    private Long id;

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

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务类型（兼容字段）")
    private String workType;

    @Schema(description = "机型")
    private String machineModel;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "产品类型")
    private String productType;

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

    @Schema(description = "状态")
    private String status;

    @Schema(description = "流程状态")
    private String flowStatus;

    @Schema(description = "展开状态")
    private String expandStatus;

    @Schema(description = "完工状态")
    private String completionStatus;

    @Schema(description = "是否订单")
    private Boolean isOrder;

    @Schema(description = "流程编码")
    private String flowCode;

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    @Schema(description = "实际开始时间")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间")
    private LocalDateTime actualEndTime;

    @Schema(description = "数据来源")
    private String dataSource;

    @Schema(description = "APS订单ID")
    private Long apsOrderId;

    @Schema(description = "APS同步批次号")
    private String apsSyncBatchId;

    @Schema(description = "APS同步状态")
    private String apsSyncStatus;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "修改人")
    private String updatedBy;

    @Schema(description = "修改时间")
    private LocalDateTime updatedTime;
}
