package com.mes.basic.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工作中心实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_work_center")
public class WorkCenter extends BaseEntity {

    /** 工作中心编码 */
    private String workCenterCode;

    /** 工作中心名称 */
    private String workCenterName;

    /** 工作中心分类 */
    private String workCenterCategory;

    /** 业务单元 */
    private String businessUnit;

    /** 工作日历 */
    private String workCalendar;

    /** 资源排序 */
    private Integer resourceOrder;

    /** 使用量 */
    private BigDecimal usageQty;

    /** 使用量单位 */
    private String usageUnit;

    /** 处理批量 */
    private BigDecimal batchQty;

    /** 效率 */
    private BigDecimal efficiency;

    /** 资源种类 */
    private String resourceType;

    /** 炉资源类型 */
    private String furnaceResourceType;

    /** 资源能力 */
    private BigDecimal resourceCapacity;

    /** 工序不中断 */
    private Integer processNoInterrupt;

    /** 工序不跨天 */
    private Integer processNoCrossDay;

    /** 固定节拍点生产 */
    private Integer fixedTaktProduction;
}
