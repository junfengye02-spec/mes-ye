package com.mes.process.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 制造BOM返回 VO
 */
@Data
@Schema(description = "制造BOM信息")
public class ManufacturingBomVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "BOM编码")
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

    @Schema(description = "BOM版本")
    private String bomVersion;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期")
    private LocalDate expiryDate;

    @Schema(description = "工厂组织")
    private String factoryOrg;

    @Schema(description = "来源版本ID")
    private Long upgradeFromId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "BOM明细树")
    private List<ManufacturingBomItemVO> items;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "修改人")
    private String updatedBy;

    @Schema(description = "修改时间")
    private LocalDateTime updatedTime;
}
