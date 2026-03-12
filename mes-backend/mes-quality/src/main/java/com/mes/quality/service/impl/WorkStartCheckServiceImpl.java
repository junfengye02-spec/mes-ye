package com.mes.quality.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.quality.domain.dto.WorkStartCheckDTO;
import com.mes.quality.domain.entity.WorkStartCheck;
import com.mes.quality.domain.query.WorkStartCheckQuery;
import com.mes.quality.domain.vo.WorkStartCheckVO;
import com.mes.quality.mapper.WorkStartCheckMapper;
import com.mes.quality.service.IWorkStartCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkStartCheckServiceImpl extends ServiceImpl<WorkStartCheckMapper, WorkStartCheck>
        implements IWorkStartCheckService {

    @Override
    public PageResult<WorkStartCheckVO> page(WorkStartCheckQuery query) {
        LambdaQueryWrapper<WorkStartCheck> wrapper = new LambdaQueryWrapper<WorkStartCheck>()
                .eq(query.getWorkOrderId() != null, WorkStartCheck::getWorkOrderId, query.getWorkOrderId())
                .like(StringUtils.hasText(query.getWorkOrderNo()), WorkStartCheck::getWorkOrderNo, query.getWorkOrderNo())
                .eq(StringUtils.hasText(query.getCheckStatus()), WorkStartCheck::getCheckStatus, query.getCheckStatus())
                .orderByDesc(WorkStartCheck::getCreatedTime);

        Page<WorkStartCheck> page = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<WorkStartCheckVO> voList = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public WorkStartCheckVO getDetail(Long id) {
        WorkStartCheck entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(WorkStartCheckDTO dto) {
        WorkStartCheck entity = new WorkStartCheck();
        BeanUtils.copyProperties(dto, entity);
        entity.setChecker("system"); // TODO: SecurityContext
        entity.setCheckTime(LocalDateTime.now());
        save(entity);

        log.info("新增工作开工检查: workOrderId={}, checkItem={}, status={}",
                entity.getWorkOrderId(), entity.getCheckItem(), entity.getCheckStatus());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, WorkStartCheckDTO dto) {
        WorkStartCheck existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);

        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        existing.setCheckTime(LocalDateTime.now());
        updateById(existing);

        log.info("修改工作开工检查: id={}", id);
    }

    private WorkStartCheckVO toVO(WorkStartCheck entity) {
        WorkStartCheckVO vo = new WorkStartCheckVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
