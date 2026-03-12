package com.mes.quality.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.quality.domain.dto.RecheckRequestDTO;
import com.mes.quality.domain.entity.RecheckRequest;
import com.mes.quality.domain.query.RecheckRequestQuery;
import com.mes.quality.domain.vo.RecheckRequestVO;

public interface IRecheckRequestService extends IService<RecheckRequest> {
    PageResult<RecheckRequestVO> page(RecheckRequestQuery query);
    RecheckRequestVO getDetail(Long id);
    Long create(RecheckRequestDTO dto);
    void update(Long id, RecheckRequestDTO dto);
    void delete(Long id);
}
