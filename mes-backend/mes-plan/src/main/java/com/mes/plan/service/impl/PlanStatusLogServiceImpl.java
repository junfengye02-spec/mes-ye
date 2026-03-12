package com.mes.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageQuery;
import com.mes.common.core.PageResult;
import com.mes.plan.domain.entity.PlanStatusLog;
import com.mes.plan.domain.vo.PlanStatusLogVO;
import com.mes.plan.mapper.PlanStatusLogMapper;
import com.mes.plan.service.IPlanStatusLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 计划状态日志 Service 实现
 */
@Slf4j
@Service
public class PlanStatusLogServiceImpl extends ServiceImpl<PlanStatusLogMapper, PlanStatusLog>
        implements IPlanStatusLogService {

    @Override
    public void log(String planType, Long planId, String fromStatus, String toStatus,
                    String action, String remark) {
        PlanStatusLog statusLog = new PlanStatusLog();
        statusLog.setPlanType(planType);
        statusLog.setPlanId(planId);
        statusLog.setFromStatus(fromStatus);
        statusLog.setToStatus(toStatus);
        statusLog.setAction(action);
        statusLog.setOperator("system"); // TODO: 从 SecurityContext 获取当前用户
        statusLog.setOperatedTime(LocalDateTime.now());
        statusLog.setRemark(remark);
        save(statusLog);
    }

    @Override
    public PageResult<PlanStatusLogVO> getLogsByPlanId(String planType, Long planId, PageQuery query) {
        Page<PlanStatusLog> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<PlanStatusLog>()
                        .eq(PlanStatusLog::getPlanType, planType)
                        .eq(PlanStatusLog::getPlanId, planId)
                        .orderByDesc(PlanStatusLog::getOperatedTime)
        );

        List<PlanStatusLogVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    private PlanStatusLogVO toVO(PlanStatusLog entity) {
        PlanStatusLogVO vo = new PlanStatusLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
