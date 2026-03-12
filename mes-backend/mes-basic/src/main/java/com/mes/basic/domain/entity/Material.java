package com.mes.basic.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 物料档案实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_material")
public class Material extends BaseEntity {

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 物料类型 */
    private String materialType;

    /** 一级分类 */
    private String categoryLevel1;

    /** 二级分类 */
    private String categoryLevel2;

    /** G编码 */
    private String gCode;

    /** 产品类型 */
    private String productType;

    /** 产品类别 */
    private String productCategory;

    /** 机型 */
    private String machineModel;

    /** 部件名称 */
    private String partName;

    /** 工厂 */
    private String factory;

    /** 基本计量单位 */
    private String baseUnit;

    /** 物料追溯方式（SERIAL/BATCH/QUANTITY） */
    private String traceMode;

    /** 序列号生成器 */
    private String serialGenerator;

    /** 批号生成器 */
    private String batchGenerator;

    /** 条码类型 */
    private String barcodeType;

    /** 是否需要检验 */
    private Integer needInspection;

    /** 图号 */
    private String drawingNo;

    /** 材料牌号 */
    private String materialBrand;

    /** 产品外观图片 */
    private String productImage;
}
