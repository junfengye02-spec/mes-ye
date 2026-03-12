package com.mes.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageQuery;
import com.mes.common.core.PageResult;
import com.mes.workorder.domain.entity.WorkOrderStatusLog;
import com.mes.workorder.domain.vo.WorkOrderStatusLogVO;
import com.mes.workorder.mapper.WorkOrderStatusLogMapper;
import com.mes.workorder.service.IWorkOrderStatusLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class WorkOrderStatusLogServiceImpl extends ServiceImpl<WorkOrderStatusLogMapper, WorkOrderStatusLog>
        implements IWorkOrderStatusLogService {

    @Override
    public void log(Long workOrderId, String fromStatus, String toStatus, String action, String remark) {
        WorkOrderStatusLog statusLog = new WorkOrderStatusLog();
        statusLog.setWorkOrderId(workOrderId);
        statusLog.setFromStatus(fromStatus);
        statusLog.setToStatus(toStatus);
        statusLog.setAction(action);
        statusLog.setOperator("system"); // TODO: 从 SecurityContext 获取当前用户
        statusLog.setOperatedTime(LocalDateTime.now());
        statusLog.setRemark(remark);
        save(statusLog);
    }

    @Override
    public PageResult<WorkOrderStatusLogVO> getLogsByWorkOrderId(Long workOrderId, PageQuery query) {
        Page<WorkOrderStatusLog> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<WorkOrderStatusLog>()
                        .eq(WorkOrderStatusLog::getWorkOrderId, workOrderId)
                        .orderByDesc(WorkOrderStatusLog::getOperatedTime)
        );

        List<WorkOrderStatusLogVO> voList = page.getRecords().stream()
                .map(entity -> {
                    WorkOrderStatusLogVO vo = new WorkOrderStatusLogVO();
                    BeanUtils.copyProperties(entity, vo);
                    return vo;
                })
                .toList();

        return PageResult.of(voList, page.getTotal());
    }
}
