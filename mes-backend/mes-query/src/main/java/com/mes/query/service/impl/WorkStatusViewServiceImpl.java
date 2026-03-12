package com.mes.query.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mes.common.core.PageResult;
import com.mes.query.domain.entity.WorkStatusView;
import com.mes.query.domain.query.WorkStatusViewQuery;
import com.mes.query.domain.vo.WorkStatusViewVO;
import com.mes.query.mapper.WorkStatusViewMapper;
import com.mes.query.service.IWorkStatusViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkStatusViewServiceImpl implements IWorkStatusViewService {

    private final WorkStatusViewMapper workStatusViewMapper;

    @Override
    public PageResult<WorkStatusViewVO> page(WorkStatusViewQuery query) {
        LambdaQueryWrapper<WorkStatusView> wrapper = new LambdaQueryWrapper<WorkStatusView>()
                .eq(StringUtils.hasText(query.getStatus()), WorkStatusView::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getWorkNo()), WorkStatusView::getWorkNo, query.getWorkNo())
                .like(StringUtils.hasText(query.getWorkName()), WorkStatusView::getWorkName, query.getWorkName())
                .eq(StringUtils.hasText(query.getFactory()), WorkStatusView::getFactory, query.getFactory())
                .eq(StringUtils.hasText(query.getBusinessOrg()), WorkStatusView::getBusinessOrg, query.getBusinessOrg())
                .orderByAsc(WorkStatusView::getSequenceNo);

        Page<WorkStatusView> page = workStatusViewMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        List<WorkStatusViewVO> voList = page.getRecords().stream().map(e -> {
            WorkStatusViewVO vo = new WorkStatusViewVO();
            BeanUtils.copyProperties(e, vo);
            return vo;
        }).toList();

        return PageResult.of(voList, page.getTotal());
    }
}
