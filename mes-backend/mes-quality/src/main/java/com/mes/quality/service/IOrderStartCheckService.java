package com.mes.quality.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.quality.domain.dto.OrderStartCheckDTO;
import com.mes.quality.domain.entity.OrderStartCheck;
import com.mes.quality.domain.query.OrderStartCheckQuery;
import com.mes.quality.domain.vo.OrderStartCheckVO;

public interface IOrderStartCheckService extends IService<OrderStartCheck> {
    PageResult<OrderStartCheckVO> page(OrderStartCheckQuery query);
    OrderStartCheckVO getDetail(Long id);
    Long create(OrderStartCheckDTO dto);
    void update(Long id, OrderStartCheckDTO dto);
}
