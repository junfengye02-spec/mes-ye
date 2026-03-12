package com.mes.workorder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageQuery;
import com.mes.common.core.PageResult;
import com.mes.workorder.domain.entity.WorkOrderStatusLog;
import com.mes.workorder.domain.vo.WorkOrderStatusLogVO;

/**
 * 工单状态日志 Service
 */
public interface IWorkOrderStatusLogService extends IService<WorkOrderStatusLog> {

    void log(Long workOrderId, String fromStatus, String toStatus, String action, String remark);

    PageResult<WorkOrderStatusLogVO> getLogsByWorkOrderId(Long workOrderId, PageQuery query);
}
