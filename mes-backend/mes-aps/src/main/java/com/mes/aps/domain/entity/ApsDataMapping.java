package com.mes.aps.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * APS数据映射表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_aps_data_mapping")
public class ApsDataMapping extends BaseEntity {

    /** 映射类型 */
    private String mappingType;
    /** MES编码 */
    private String mesCode;
    /** MES名称 */
    private String mesName;
    /** APS编码 */
    private String apsCode;
    /** APS名称 */
    private String apsName;
    /** 是否启用 */
    private Integer enabled;
}
