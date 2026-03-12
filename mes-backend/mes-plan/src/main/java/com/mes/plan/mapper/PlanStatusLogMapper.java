package com.mes.plan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.plan.domain.entity.PlanStatusLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 计划状态日志 Mapper
 */
@Mapper
public interface PlanStatusLogMapper extends BaseMapper<PlanStatusLog> {
}
