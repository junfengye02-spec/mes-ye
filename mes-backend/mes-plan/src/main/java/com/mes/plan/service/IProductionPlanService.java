package com.mes.plan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.plan.domain.dto.ProductionPlanDTO;
import com.mes.plan.domain.entity.ProductionPlan;
import com.mes.plan.domain.query.ProductionPlanQuery;
import com.mes.plan.domain.vo.ProductionPlanVO;

/**
 * 生产计划 Service
 */
public interface IProductionPlanService extends IService<ProductionPlan> {

    /**
     * 分页查询
     */
    PageResult<ProductionPlanVO> page(ProductionPlanQuery query);

    /**
     * 获取详情
     */
    ProductionPlanVO getDetail(Long id);

    /**
     * 新增
     */
    Long create(ProductionPlanDTO dto);

    /**
     * 修改
     */
    void update(Long id, ProductionPlanDTO dto);

    /**
     * 删除
     */
    void delete(Long id);

    /**
     * 下达（自动创建工单）
     */
    void release(Long id);
}
