package com.mes.material.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.material.domain.dto.RequisitionOrderDTO;
import com.mes.material.domain.entity.RequisitionOrder;
import com.mes.material.domain.query.RequisitionOrderQuery;
import com.mes.material.domain.vo.RequisitionOrderVO;

public interface IRequisitionOrderService extends IService<RequisitionOrder> {
    PageResult<RequisitionOrderVO> page(RequisitionOrderQuery query);
    RequisitionOrderVO getDetail(Long id);
    Long create(RequisitionOrderDTO dto);
    void update(Long id, RequisitionOrderDTO dto);
    void delete(Long id);
}
