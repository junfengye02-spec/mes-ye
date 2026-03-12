package com.mes.dispatch.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.dispatch.domain.entity.DispatchStatusLog;
import com.mes.dispatch.mapper.DispatchStatusLogMapper;
import com.mes.dispatch.service.IDispatchStatusLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class DispatchStatusLogServiceImpl extends ServiceImpl<DispatchStatusLogMapper, DispatchStatusLog>
        implements IDispatchStatusLogService {

    @Override
    public void log(Long dispatchTaskId, String fromStatus, String toStatus, String action, String remark) {
        DispatchStatusLog statusLog = new DispatchStatusLog();
        statusLog.setDispatchTaskId(dispatchTaskId);
        statusLog.setFromStatus(fromStatus);
        statusLog.setToStatus(toStatus);
        statusLog.setAction(action);
        statusLog.setOperator("system"); // TODO: 从 SecurityContext 获取当前用户
        statusLog.setOperatedTime(LocalDateTime.now());
        statusLog.setRemark(remark);
        save(statusLog);
    }
}
