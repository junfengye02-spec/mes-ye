package com.mes.process.service.support;

import lombok.Builder;
import lombok.Data;

/**
 * 通用工艺参数模板定义
 */
@Data
@Builder
public class ProcessParameterSchemaDefinition {

    private String schemaCode;
    private String schemaName;
    private String processType;
    private String fieldDefinitionsJson;
}
