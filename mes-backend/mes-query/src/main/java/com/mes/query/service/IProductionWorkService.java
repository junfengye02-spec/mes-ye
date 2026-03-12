package com.mes.query.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.query.domain.entity.ProductionWork;
import com.mes.query.domain.query.ProductionWorkQuery;
import com.mes.query.domain.vo.ProductionWorkVO;

public interface IProductionWorkService extends IService<ProductionWork> {
    PageResult<ProductionWorkVO> page(ProductionWorkQuery query);
    ProductionWorkVO getDetail(Long id);
}
