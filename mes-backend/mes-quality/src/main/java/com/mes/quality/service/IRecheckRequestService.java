package com.mes.quality.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.quality.domain.dto.RecheckApproveDTO;
import com.mes.quality.domain.dto.RecheckRequestDTO;
import com.mes.quality.domain.dto.RecheckReviewDTO;
import com.mes.quality.domain.entity.RecheckRequest;
import com.mes.quality.domain.query.RecheckRequestQuery;
import com.mes.quality.domain.vo.RecheckRequestVO;

public interface IRecheckRequestService extends IService<RecheckRequest> {
    PageResult<RecheckRequestVO> page(RecheckRequestQuery query);
    RecheckRequestVO getDetail(Long id);
    Long create(RecheckRequestDTO dto);
    void update(Long id, RecheckRequestDTO dto);
    void submit(Long id);
    void review(Long id, RecheckReviewDTO dto);
    void approve(Long id, RecheckApproveDTO dto);
    void complete(Long id);
    void delete(Long id);
}
