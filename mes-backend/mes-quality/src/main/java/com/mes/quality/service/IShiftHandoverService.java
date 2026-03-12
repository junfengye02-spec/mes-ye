package com.mes.quality.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.quality.domain.dto.ShiftHandoverDTO;
import com.mes.quality.domain.entity.ShiftHandover;
import com.mes.quality.domain.query.ShiftHandoverQuery;
import com.mes.quality.domain.vo.ShiftHandoverVO;

public interface IShiftHandoverService extends IService<ShiftHandover> {
    PageResult<ShiftHandoverVO> page(ShiftHandoverQuery query);
    ShiftHandoverVO getDetail(Long id);
    Long create(ShiftHandoverDTO dto);
    void update(Long id, ShiftHandoverDTO dto);
    void receive(Long id);
}
