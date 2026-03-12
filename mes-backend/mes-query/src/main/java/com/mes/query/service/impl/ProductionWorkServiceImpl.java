package com.mes.query.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.query.domain.entity.ProductionWork;
import com.mes.query.domain.query.ProductionWorkQuery;
import com.mes.query.domain.vo.ProductionWorkVO;
import com.mes.query.mapper.ProductionWorkMapper;
import com.mes.query.service.IProductionWorkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class ProductionWorkServiceImpl extends ServiceImpl<ProductionWorkMapper, ProductionWork>
        implements IProductionWorkService {

    @Override
    public PageResult<ProductionWorkVO> page(ProductionWorkQuery query) {
        LambdaQueryWrapper<ProductionWork> wrapper = new LambdaQueryWrapper<ProductionWork>()
                .like(StringUtils.hasText(query.getWorkNo()), ProductionWork::getWorkNo, query.getWorkNo())
                .like(StringUtils.hasText(query.getWorkName()), ProductionWork::getWorkName, query.getWorkName())
                .like(StringUtils.hasText(query.getWorkOrderNo()), ProductionWork::getWorkOrderNo, query.getWorkOrderNo())
                .eq(query.getWorkOrderId() != null, ProductionWork::getWorkOrderId, query.getWorkOrderId())
                .orderByDesc(ProductionWork::getCreatedTime);

        Page<ProductionWork> page = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<ProductionWorkVO> voList = page.getRecords().stream().map(e -> {
            ProductionWorkVO vo = new ProductionWorkVO();
            BeanUtils.copyProperties(e, vo);
            return vo;
        }).toList();
        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public ProductionWorkVO getDetail(Long id) {
        ProductionWork entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        ProductionWorkVO vo = new ProductionWorkVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
