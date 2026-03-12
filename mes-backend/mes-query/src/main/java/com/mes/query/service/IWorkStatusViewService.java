package com.mes.query.service;

import com.mes.common.core.PageResult;
import com.mes.query.domain.query.WorkStatusViewQuery;
import com.mes.query.domain.vo.WorkStatusViewVO;

public interface IWorkStatusViewService {
    PageResult<WorkStatusViewVO> page(WorkStatusViewQuery query);
}
