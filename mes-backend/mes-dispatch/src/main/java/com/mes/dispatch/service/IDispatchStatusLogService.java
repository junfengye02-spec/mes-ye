package com.mes.dispatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.dispatch.domain.entity.DispatchStatusLog;

/**
 * 派工状态日志 Service
 */
public interface IDispatchStatusLogService extends IService<DispatchStatusLog> {

    void log(Long dispatchTaskId, String fromStatus, String toStatus, String action, String remark);
}
