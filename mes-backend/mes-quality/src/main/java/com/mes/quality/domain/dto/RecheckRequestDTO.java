package com.mes.quality.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 复检申请 DTO
 */
@Data
@Schema(description = "复检申请请求参数")
public class RecheckRequestDTO {

    @Schema(description = "项目编码")
    private String projectCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "生产订单")
    private String productionOrderNo;

    @Schema(description = "复检需求")
    private String recheckRequirement;

    @Schema(description = "复检原因")
    private String recheckReason;

    @Schema(description = "复检提出人")
    private String recheckProposer;

    @Schema(description = "复检提出时间")
    private LocalDateTime recheckProposeTime;

    @Schema(description = "需求交货时间")
    private LocalDateTime requiredDeliveryTime;

    @Schema(description = "订单计划关联列表")
    private List<RecheckOrderPlanDTO> orderPlans;

    @Schema(description = "产品序列号列表")
    private List<RecheckSerialDTO> serials;
}
