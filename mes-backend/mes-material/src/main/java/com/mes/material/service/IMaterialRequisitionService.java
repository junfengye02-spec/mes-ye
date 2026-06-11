package com.mes.material.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.material.domain.dto.MaterialRequisitionDTO;
import com.mes.material.domain.entity.MaterialRequisition;
import com.mes.material.domain.query.MaterialRequisitionQuery;
import com.mes.material.domain.vo.MaterialRequisitionVO;

public interface IMaterialRequisitionService extends IService<MaterialRequisition> {
    PageResult<MaterialRequisitionVO> page(MaterialRequisitionQuery query);
    MaterialRequisitionVO getDetail(Long id);
    Long create(MaterialRequisitionDTO dto);
    Long createFromMrp(MaterialRequisitionDTO dto);
    void update(Long id, MaterialRequisitionDTO dto);
    void delete(Long id);
}
