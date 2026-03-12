package com.mes.material.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.material.domain.dto.FinishedGoodsReceiptDTO;
import com.mes.material.domain.entity.FinishedGoodsReceipt;
import com.mes.material.domain.query.FinishedGoodsReceiptQuery;
import com.mes.material.domain.vo.FinishedGoodsReceiptVO;

public interface IFinishedGoodsReceiptService extends IService<FinishedGoodsReceipt> {
    PageResult<FinishedGoodsReceiptVO> page(FinishedGoodsReceiptQuery query);
    FinishedGoodsReceiptVO getDetail(Long id);
    Long create(FinishedGoodsReceiptDTO dto);
    void update(Long id, FinishedGoodsReceiptDTO dto);
    void delete(Long id);
}
