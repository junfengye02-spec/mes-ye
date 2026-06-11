package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通用工艺参数模板定义
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_process_parameter_schema")
public class ProcessParameterSchema extends BaseEntity {

    /** 模板编码 */
    private String schemaCode;

    /** 模板名称 */
    private String schemaName;

    /** 适用工艺类型 */
    private String processType;

    /** 字段定义 JSON */
    @TableField("field_definitions")
    private String fieldDefinitionsJson;
}
