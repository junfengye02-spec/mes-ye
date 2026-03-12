package com.mes.process.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 工序模板新增/编辑 DTO
 */
@Data
@Schema(description = "工序模板请求参数")
public class ProcessTemplateDTO {

    @NotBlank(message = "工序号不能为空")
    @Schema(description = "工序号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String processNo;

    @Schema(description = "工序名")
    private String processName;

    @Schema(description = "父工序号")
    private String parentProcessNo;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "机型")
    private String machineModel;

    @Schema(description = "产品类型")
    private String productType;

    @Schema(description = "工序类型（生产工序/检验工序）")
    private String processType;

    @Schema(description = "工序过程表单")
    private String processForm;

    @Schema(description = "加工图纸")
    private String processDrawing;

    @Schema(description = "工作中心ID")
    private Long workCenterId;

    @Schema(description = "处理时间")
    private BigDecimal handleTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "说明")
    private String description;
}
