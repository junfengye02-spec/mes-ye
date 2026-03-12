package com.mes.material.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 存储地点库存表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_storage_inventory")
public class StorageInventory extends BaseEntity {

    /** 工厂 */
    private String factory;
    /** 存货组织 */
    private String inventoryOrg;
    /** 仓库 */
    private String warehouse;
    /** 存储地点 */
    private String storageLocation;
    /** 物料ID */
    private Long materialId;
    /** 物料编码 */
    private String materialCode;
    /** 物料名称 */
    private String materialName;
    /** 非限制库存 */
    private BigDecimal unrestrictedStock;
    /** 质检库存 */
    private BigDecimal qualityStock;
    /** 冻结库存 */
    private BigDecimal frozenStock;
    /** 计量单位 */
    private String unit;
    /** 班组ID */
    private Long teamId;
}
