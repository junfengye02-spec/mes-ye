package com.mes.material.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.material.domain.dto.MaterialReturnDTO;
import com.mes.material.domain.entity.MaterialReturn;
import com.mes.material.domain.query.MaterialReturnQuery;
import com.mes.material.domain.vo.MaterialReturnVO;

public interface IMaterialReturnService extends IService<MaterialReturn> {
    PageResult<MaterialReturnVO> page(MaterialReturnQuery query);
    MaterialReturnVO getDetail(Long id);
    Long create(MaterialReturnDTO dto);
    void update(Long id, MaterialReturnDTO dto);
    void delete(Long id);
}
