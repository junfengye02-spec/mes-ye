package com.mes.plan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.plan.domain.entity.OrderPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单计划 Mapper
 */
@Mapper
public interface OrderPlanMapper extends BaseMapper<OrderPlan> {
}
