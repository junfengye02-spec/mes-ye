package com.mes.quality.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.quality.domain.dto.WorkStartCheckDTO;
import com.mes.quality.domain.entity.WorkStartCheck;
import com.mes.quality.domain.query.WorkStartCheckQuery;
import com.mes.quality.domain.vo.WorkStartCheckVO;

public interface IWorkStartCheckService extends IService<WorkStartCheck> {
    PageResult<WorkStartCheckVO> page(WorkStartCheckQuery query);
    WorkStartCheckVO getDetail(Long id);
    Long create(WorkStartCheckDTO dto);
    void update(Long id, WorkStartCheckDTO dto);
}
