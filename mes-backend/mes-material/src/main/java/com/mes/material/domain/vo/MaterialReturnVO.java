package com.mes.material.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "退料申请信息")
public class MaterialReturnVO {
    @Schema(description = "ID") private Long id;
    @Schema(description = "退料单号") private String returnNo;
    @Schema(description = "工单ID") private Long workOrderId;
    @Schema(description = "工单号") private String workOrderNo;
    @Schema(description = "订单号") private String orderNo;
    @Schema(description = "产品编码") private String productCode;
    @Schema(description = "产品名称") private String productName;
    @Schema(description = "项目") private String projectName;
    @Schema(description = "新制维修类型") private String newOrRepairType;
    @Schema(description = "业务类型") private String businessType;
    @Schema(description = "业务类型（兼容字段）") private String workType;
    @Schema(description = "流程编码") private String flowCode;
    @Schema(description = "计划数量") private BigDecimal planQty;
    @Schema(description = "完工数量") private BigDecimal completedQty;
    @Schema(description = "状态") private String status;
    @Schema(description = "流程状态") private String flowStatus;
    @Schema(description = "创建人") private String createdBy;
    @Schema(description = "创建时间") private LocalDateTime createdTime;
}
