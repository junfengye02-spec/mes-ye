package com.mes.query.service;

import com.mes.common.event.DispatchTaskCompletedEvent;
import com.mes.common.event.RecheckCompletedEvent;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.query.domain.entity.InspectionWork;
import com.mes.query.domain.query.InspectionWorkQuery;
import com.mes.query.domain.vo.InspectionWorkVO;

public interface IInspectionWorkService extends IService<InspectionWork> {
    PageResult<InspectionWorkVO> page(InspectionWorkQuery query);
    InspectionWorkVO getDetail(Long id);
    void projectDispatchCompletion(DispatchTaskCompletedEvent event);
    void projectRecheckCompletion(RecheckCompletedEvent event);
}
