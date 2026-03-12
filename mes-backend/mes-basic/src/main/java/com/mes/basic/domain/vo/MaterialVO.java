package com.mes.basic.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物料档案返回 VO
 */
@Data
@Schema(description = "物料档案信息")
public class MaterialVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "物料类型")
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

    @Schema(description = "基本计量单位")
    private String baseUnit;

    @Schema(description = "物料追溯方式")
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

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "修改人")
    private String updatedBy;

    @Schema(description = "修改时间")
    private LocalDateTime updatedTime;
}
