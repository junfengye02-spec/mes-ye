package com.mes.query.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.query.domain.entity.InspectionWork;
import com.mes.query.domain.query.InspectionWorkQuery;
import com.mes.query.domain.vo.InspectionWorkVO;
import com.mes.query.mapper.InspectionWorkMapper;
import com.mes.query.service.IInspectionWorkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class InspectionWorkServiceImpl extends ServiceImpl<InspectionWorkMapper, InspectionWork>
        implements IInspectionWorkService {

    @Override
    public PageResult<InspectionWorkVO> page(InspectionWorkQuery query) {
        LambdaQueryWrapper<InspectionWork> wrapper = new LambdaQueryWrapper<InspectionWork>()
                .like(StringUtils.hasText(query.getWorkNo()), InspectionWork::getWorkNo, query.getWorkNo())
                .like(StringUtils.hasText(query.getWorkName()), InspectionWork::getWorkName, query.getWorkName())
                .eq(StringUtils.hasText(query.getWorkStatus()), InspectionWork::getWorkStatus, query.getWorkStatus())
                .eq(query.getWorkOrderId() != null, InspectionWork::getWorkOrderId, query.getWorkOrderId())
                .eq(StringUtils.hasText(query.getInspectCategory()), InspectionWork::getInspectCategory, query.getInspectCategory())
                .orderByDesc(InspectionWork::getCreatedTime);

        Page<InspectionWork> page = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<InspectionWorkVO> voList = page.getRecords().stream().map(e -> {
            InspectionWorkVO vo = new InspectionWorkVO();
            BeanUtils.copyProperties(e, vo);
            return vo;
        }).toList();
        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public InspectionWorkVO getDetail(Long id) {
        InspectionWork entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        InspectionWorkVO vo = new InspectionWorkVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
