package com.mes.basic.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 物料档案新增/编辑 DTO
 */
@Data
@Schema(description = "物料档案请求参数")
public class MaterialDTO {

    @NotBlank(message = "物料编码不能为空")
    @Schema(description = "物料编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String materialCode;

    @NotBlank(message = "物料名称不能为空")
    @Schema(description = "物料名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String materialName;

    @NotBlank(message = "物料类型不能为空")
    @Schema(description = "物料类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String materialType;

    @Schema(description = "一级分类")
    private String categoryLevel1;

    @Schema(description = "二级分类")
    private String categoryLevel2;

    @Schema(description = "G编码")
    private String gCode;

    @Schema(description = "产品类型")
    private String productType;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "机型")
    private String machineModel;

    @Schema(description = "部件名称")
    private String partName;

    @Schema(description = "工厂")
    private String factory;

    @NotBlank(message = "基本计量单位不能为空")
    @Schema(description = "基本计量单位", requiredMode = Schema.RequiredMode.REQUIRED)
    private String baseUnit;

    @Schema(description = "物料追溯方式（SERIAL/BATCH/QUANTITY）")
    private String traceMode;

    @Schema(description = "序列号生成器")
    private String serialGenerator;

    @Schema(description = "批号生成器")
    private String batchGenerator;

    @Schema(description = "条码类型")
    private String barcodeType;

    @Schema(description = "是否需要检验")
    private Integer needInspection;

    @Schema(description = "图号")
    private String drawingNo;

    @Schema(description = "材料牌号")
    private String materialBrand;

    @Schema(description = "产品外观图片")
    private String productImage;
}
