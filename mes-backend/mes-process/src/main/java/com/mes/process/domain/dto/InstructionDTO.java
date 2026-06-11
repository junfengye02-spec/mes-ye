package com.mes.process.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 指示书新增/编辑 DTO
 * <p>面向具体执行单据，允许引用一份标准化作业指导书模板。</p>
 */
@Data
@Schema(description = "指示书请求参数（随工单执行指示/流转卡）")
public class InstructionDTO {

    @NotBlank(message = "指示书号不能为空")
    @Schema(description = "指示书号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String instructionNo;

    @Schema(description = "项目编号")
    private String projectNo;

    @Schema(description = "WBS")
    private String wbs;

    @Schema(description = "新制维修类型")
    private String newOrRepairType;

    @Schema(description = "类型（如主机）")
    private String mainType;

    @Schema(description = "扩展属性")
    private Map<String, Object> extensionData;

    @Schema(description = "G/T类型（兼容字段，请改用 extensionData.gtType）")
    private String gtType;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "产品类型")
    private String productType;

    @Schema(description = "部件名称")
    private String partName;

    @Schema(description = "生产订单编号")
    private String workOrderNo;

    @Schema(description = "关联作业指导书ID（引用可复用SOP模板）")
    private Long workInstructionId;

    @Schema(description = "生产完工日期")
    private LocalDate finishDate;

    @Schema(description = "数量")
    private Integer qty;

    @Schema(description = "发行日期")
    private LocalDate issueDate;

    @Schema(description = "产品最终交货期")
    private LocalDate finalDeliveryDate;

    @Schema(description = "检查提交日期")
    private LocalDate checkSubmitDate;

    @Schema(description = "项目·图纸号")
    private String drawingNo;

    @Schema(description = "维修指导图（兼容字段，请改用 extensionData.repairGuideDrawing）")
    private String repairGuideDrawing;

    @Schema(description = "担当")
    private String assignee;

    @Schema(description = "加工状态")
    private String processingStatus;

    @Schema(description = "原材料到货期")
    private LocalDate rawMaterialArrivalDate;

    @Schema(description = "原材料采购名义")
    private String rawMaterialPurchaseName;

    @Schema(description = "采购申请单号")
    private String purchaseRequestNo;

    @Schema(description = "接收时间")
    private LocalDateTime receiveTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "阶段内容列表")
    private List<InstructionStageDTO> stages;

    @Schema(description = "序列号列表")
    private List<InstructionSerialDTO> serials;
}
