package com.mes.quality.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 复检申请产品序列号表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_recheck_serial")
public class RecheckSerial extends BaseEntity {

    /** 复检申请ID */
    private Long recheckId;

    /** 序列号 */
    private String serialNo;

    /** 生产厂商 */
    private String manufacturer;

    /** 名称 */
    private String name;

    /** 状态分类 */
    private String statusCategory;

    /** 数量 */
    private BigDecimal qty;

    /** 冻结 */
    private Integer frozen;

    /** 拆分完成 */
    private Integer splitCompleted;

    /** 计量单位 */
    private String unit;

    /** 条码号 */
    private String barcode;
}
