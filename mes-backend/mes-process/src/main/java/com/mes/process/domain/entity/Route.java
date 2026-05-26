package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 工艺路线主表实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_route")
public class Route extends BaseEntity {

    /** 路线编码 */
    private String routeCode;

    /** 路线名称 */
    private String routeName;

    /** 产品编码 */
    private String productCode;

    /** 产品类别 */
    private String productCategory;

    /** 机型 */
    private String machineModel;

    /** 产品类型 */
    private String productType;

    /** 状态 */
    private String status;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 失效日期 */
    private LocalDate expiryDate;

    /** 备注 */
    private String remark;
}
