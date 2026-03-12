package com.mes.process.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.process.domain.dto.SprayConditionDTO;
import com.mes.process.domain.entity.SprayCondition;
import com.mes.process.domain.query.SprayConditionQuery;
import com.mes.process.domain.vo.SprayConditionVO;

/**
 * 喷涂条件 Service 接口
 */
public interface ISprayConditionService extends IService<SprayCondition> {

    /** 分页查询 */
    PageResult<SprayConditionVO> page(SprayConditionQuery query);

    /** 获取详情 */
    SprayConditionVO getDetail(Long id);

    /** 新增 */
    Long create(SprayConditionDTO dto);

    /** 修改 */
    void update(Long id, SprayConditionDTO dto);

    /** 删除 */
    void delete(Long id);
}
