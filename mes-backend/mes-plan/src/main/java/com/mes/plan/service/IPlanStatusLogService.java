package com.mes.plan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageQuery;
import com.mes.common.core.PageResult;
import com.mes.plan.domain.entity.PlanStatusLog;
import com.mes.plan.domain.vo.PlanStatusLogVO;

/**
 * 计划状态日志 Service
 */
public interface IPlanStatusLogService extends IService<PlanStatusLog> {

    /**
     * 记录状态变更日志
     */
    void log(String planType, Long planId, String fromStatus, String toStatus,
             String action, String remark);

    /**
     * 查询状态日志
     */
    PageResult<PlanStatusLogVO> getLogsByPlanId(String planType, Long planId, PageQuery query);
}
