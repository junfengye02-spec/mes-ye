package com.mes.abnormal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.abnormal.domain.entity.AbnormalContactLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 异常联络单状态日志 Mapper
 */
@Mapper
public interface AbnormalContactLogMapper extends BaseMapper<AbnormalContactLog> {
}
