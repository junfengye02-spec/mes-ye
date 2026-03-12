package com.mes.material.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.material.domain.dto.ReceiptRequestDTO;
import com.mes.material.domain.entity.FinishedGoodsReceiptRequest;
import com.mes.material.domain.query.ReceiptRequestQuery;
import com.mes.material.domain.vo.ReceiptRequestVO;

public interface IReceiptRequestService extends IService<FinishedGoodsReceiptRequest> {
    PageResult<ReceiptRequestVO> page(ReceiptRequestQuery query);
    ReceiptRequestVO getDetail(Long id);
    Long create(ReceiptRequestDTO dto);
    void update(Long id, ReceiptRequestDTO dto);
    void delete(Long id);
}
