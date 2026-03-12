package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 制造BOM主表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_manufacturing_bom")
public class ManufacturingBom extends BaseEntity {

    /** BOM编码 */
    private String bomCode;

    /** BOM名称 */
    private String bomName;

    /** 产品ID */
    private Long productId;

    /** 产品编码 */
    private String productCode;

    /** 产品名称 */
    private String productName;

    /** 产品类别 */
    private String productCategory;

    /** 机型 */
    private String machineModel;

    /** 产品类型 */
    private String productType;

    /** 新制维修类型 */
    private String newOrRepairType;

    /** BOM版本 */
    private String bomVersion;

    /** 状态 */
    private String status;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 失效日期 */
    private LocalDate expiryDate;

    /** 工厂组织 */
    private String factoryOrg;

    /** 来源版本ID */
    private Long upgradeFromId;

    /** 备注 */
    private String remark;
}
