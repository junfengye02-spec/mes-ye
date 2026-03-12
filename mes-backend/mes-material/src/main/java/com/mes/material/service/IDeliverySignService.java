package com.mes.material.service;

import com.mes.common.core.PageResult;
import com.mes.material.domain.dto.DeliverySignDTO;
import com.mes.material.domain.query.DeliverySignQuery;
import com.mes.material.domain.vo.DeliverySignVO;

public interface IDeliverySignService {
    PageResult<DeliverySignVO> page(DeliverySignQuery query);
    Long create(DeliverySignDTO dto);
    void confirm(Long id);
}
