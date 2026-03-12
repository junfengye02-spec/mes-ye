package com.mes.workorder.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "工单状态日志视图对象")
public class WorkOrderStatusLogVO {

    private Long id;
    private Long workOrderId;
    private String fromStatus;
    private String toStatus;
    private String action;
    private String operator;
    private LocalDateTime operatedTime;
    private String remark;
}
