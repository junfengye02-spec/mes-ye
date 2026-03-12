package com.mes.plan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.plan.domain.dto.OrderPlanDTO;
import com.mes.plan.domain.entity.OrderPlan;
import com.mes.plan.domain.query.OrderPlanQuery;
import com.mes.plan.domain.vo.OrderPlanVO;

/**
 * 订单计划 Service
 */
public interface IOrderPlanService extends IService<OrderPlan> {

    /**
     * 分页查询
     */
    PageResult<OrderPlanVO> page(OrderPlanQuery query);

    /**
     * 获取详情
     */
    OrderPlanVO getDetail(Long id);

    /**
     * 新增
     */
    Long create(OrderPlanDTO dto);

    /**
     * 修改
     */
    void update(Long id, OrderPlanDTO dto);

    /**
     * 删除
     */
    void delete(Long id);

    /**
     * 下达
     */
    void release(Long id);

    /**
     * 完成
     */
    void complete(Long id);

    /**
     * 终止
     */
    void terminate(Long id, String reason);

    /**
     * 展开（分解为生产计划）
     */
    void expand(Long id);
}
