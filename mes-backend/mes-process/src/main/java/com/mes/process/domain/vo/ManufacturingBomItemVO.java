package com.mes.process.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 制造BOM明细返回 VO（支持树形）
 */
@Data
@Schema(description = "制造BOM明细信息")
public class ManufacturingBomItemVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "BOM主表ID")
    private Long bomId;

    @Schema(description = "父级明细ID")
    private Long parentItemId;

    @Schema(description = "层级")
    private Integer level;

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "物料规格")
    private String materialSpec;

    @Schema(description = "物料类型")
    private String materialType;

    @Schema(description = "用量")
    private BigDecimal quantity;

    @Schema(description = "损耗率(%)")
    private BigDecimal lossRate;

    @Schema(description = "计量单位")
    private String unit;

    @Schema(description = "供应类型")
    private String supplyType;

    @Schema(description = "关联工序ID")
    private Long processId;

    @Schema(description = "关联工序号")
    private String processNo;

    @Schema(description = "替代料标识")
    private Integer isSubstitute;

    @Schema(description = "替代料组")
    private String substituteGroup;

    @Schema(description = "是否关键件")
    private Integer isKeyPart;

    @Schema(description = "排序号")
    private Integer sequenceNo;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "替代料列表")
    private List<BomSubstituteVO> substitutes;

    @Schema(description = "子级明细")
    private List<ManufacturingBomItemVO> children;
}
