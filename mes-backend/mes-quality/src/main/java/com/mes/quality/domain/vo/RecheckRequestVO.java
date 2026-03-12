package com.mes.quality.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 复检申请返回 VO
 */
@Data
@Schema(description = "复检申请信息")
public class RecheckRequestVO {

    @Schema(description = "ID")
    private Long id;

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

    @Schema(description = "是否合理")
    private Integer isReasonable;

    @Schema(description = "审核人员")
    private String reviewer;

    @Schema(description = "审核日期")
    private LocalDate reviewDate;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "订单计划关联列表")
    private List<RecheckOrderPlanVO> orderPlans;

    @Schema(description = "产品序列号列表")
    private List<RecheckSerialVO> serials;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "修改人")
    private String updatedBy;

    @Schema(description = "修改时间")
    private LocalDateTime updatedTime;
}
