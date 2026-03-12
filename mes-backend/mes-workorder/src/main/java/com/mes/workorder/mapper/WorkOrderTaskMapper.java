package com.mes.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.workorder.domain.entity.WorkOrderTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkOrderTaskMapper extends BaseMapper<WorkOrderTask> {
}
