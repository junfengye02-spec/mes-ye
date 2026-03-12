package com.mes.plan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.plan.domain.entity.ProductionPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 生产计划 Mapper
 */
@Mapper
public interface ProductionPlanMapper extends BaseMapper<ProductionPlan> {
}
