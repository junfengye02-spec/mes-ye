package com.mes.aps.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.aps.domain.dto.ApsDataMappingDTO;
import com.mes.aps.domain.entity.ApsDataMapping;
import com.mes.aps.domain.query.ApsDataMappingQuery;
import com.mes.aps.domain.vo.ApsDataMappingVO;
import com.mes.common.core.PageResult;

public interface IApsDataMappingService extends IService<ApsDataMapping> {
    PageResult<ApsDataMappingVO> page(ApsDataMappingQuery query);
    ApsDataMappingVO getDetail(Long id);
    Long create(ApsDataMappingDTO dto);
    void update(Long id, ApsDataMappingDTO dto);
    void delete(Long id);

    /**
     * 通过 APS 编码查找 MES 编码
     */
    String getMesCode(String mappingType, String apsCode);

    /**
     * 通过 MES 编码查找 APS 编码
     */
    String getApsCode(String mappingType, String mesCode);
}
