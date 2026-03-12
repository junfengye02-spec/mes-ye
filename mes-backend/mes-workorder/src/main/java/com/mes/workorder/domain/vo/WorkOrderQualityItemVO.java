package com.mes.workorder.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "检验项目视图对象")
public class WorkOrderQualityItemVO {

    private Long id;
    private Long workOrderId;
    private String qualityItemCode;
    private String qualityItemName;
    private String requirement;
    private String status;
}
