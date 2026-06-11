package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通用工艺参数值
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_process_parameter_value")
public class ProcessParameterValue extends BaseEntity {

    /** 参数模板ID */
    private Long schemaId;

    /** 参数编码 */
    private String parameterCode;

    /** 参数名称 */
    private String parameterName;

    /** 关联工序ID */
    private Long processInfoId;

    /** 工艺类型 */
    private String processType;

    /** 状态 */
    private String status;

    /** 搜索辅助文本 */
    private String searchText;

    /** 参数值 JSON */
    @TableField("param_values")
    private String paramValuesJson;
}
