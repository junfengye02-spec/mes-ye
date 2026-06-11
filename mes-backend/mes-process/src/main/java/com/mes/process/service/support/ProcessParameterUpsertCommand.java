package com.mes.process.service.support;

import lombok.Builder;
import lombok.Data;

/**
 * 通用工艺参数写入命令
 */
@Data
@Builder
public class ProcessParameterUpsertCommand {

    private String parameterCode;
    private String parameterName;
    private Long processInfoId;
    private String processType;
    private String status;
    private String searchText;
    private String paramValuesJson;
}
