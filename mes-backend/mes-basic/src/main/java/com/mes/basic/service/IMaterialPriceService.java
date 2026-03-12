package com.mes.basic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.basic.domain.dto.MaterialPriceDTO;
import com.mes.basic.domain.entity.MaterialPrice;
import com.mes.basic.domain.query.MaterialPriceQuery;
import com.mes.basic.domain.vo.MaterialPriceVO;
import com.mes.common.core.PageResult;

/**
 * 物料价格 Service 接口
 */
public interface IMaterialPriceService extends IService<MaterialPrice> {

    /**
     * 分页查询物料价格
     */
    PageResult<MaterialPriceVO> page(MaterialPriceQuery query);

    /**
     * 获取物料价格详情
     */
    MaterialPriceVO getDetail(Long id);

    /**
     * 新增物料价格
     */
    Long create(MaterialPriceDTO dto);

    /**
     * 修改物料价格
     */
    void update(Long id, MaterialPriceDTO dto);

    /**
     * 删除物料价格
     */
    void delete(Long id);
}
