package com.mes.process.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 制造BOM新增/编辑 DTO
 */
@Data
@Schema(description = "制造BOM请求参数")
public class ManufacturingBomDTO {

    @NotBlank(message = "BOM编码不能为空")
    @Schema(description = "BOM编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bomCode;

    @Schema(description = "BOM名称")
    private String bomName;

    @Schema(description = "产品ID")
    private Long productId;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "机型")
    private String machineModel;

    @Schema(description = "产品类型")
    private String productType;

    @Schema(description = "新制维修类型")
    private String newOrRepairType;

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期")
    private LocalDate expiryDate;

    @Schema(description = "工厂组织")
    private String factoryOrg;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "BOM明细列表（支持嵌套子项）")
    private List<ManufacturingBomItemDTO> items;
}
