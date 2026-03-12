package com.mes.dispatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.dispatch.domain.entity.DispatchTask;
import com.mes.dispatch.domain.query.DispatchTaskQuery;
import com.mes.dispatch.domain.vo.DispatchTaskVO;

/**
 * 派工任务 Service
 */
public interface IDispatchTaskService extends IService<DispatchTask> {

    PageResult<DispatchTaskVO> page(DispatchTaskQuery query);

    DispatchTaskVO getDetail(Long id);

    /**
     * 从工单工作清单自动生成派工任务
     */
    void generateFromWorkOrder(Long workOrderId);
}
