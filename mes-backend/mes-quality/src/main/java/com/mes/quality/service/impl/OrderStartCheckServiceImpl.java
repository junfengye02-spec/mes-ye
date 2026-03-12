package com.mes.quality.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.quality.domain.dto.OrderStartCheckDTO;
import com.mes.quality.domain.entity.OrderStartCheck;
import com.mes.quality.domain.query.OrderStartCheckQuery;
import com.mes.quality.domain.vo.OrderStartCheckVO;
import com.mes.quality.mapper.OrderStartCheckMapper;
import com.mes.quality.service.IOrderStartCheckService;
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
public class OrderStartCheckServiceImpl extends ServiceImpl<OrderStartCheckMapper, OrderStartCheck>
        implements IOrderStartCheckService {

    @Override
    public PageResult<OrderStartCheckVO> page(OrderStartCheckQuery query) {
        LambdaQueryWrapper<OrderStartCheck> wrapper = new LambdaQueryWrapper<OrderStartCheck>()
                .eq(query.getWorkOrderId() != null, OrderStartCheck::getWorkOrderId, query.getWorkOrderId())
                .like(StringUtils.hasText(query.getWorkOrderNo()), OrderStartCheck::getWorkOrderNo, query.getWorkOrderNo())
                .eq(StringUtils.hasText(query.getCheckStatus()), OrderStartCheck::getCheckStatus, query.getCheckStatus())
                .orderByDesc(OrderStartCheck::getCreatedTime);

        Page<OrderStartCheck> page = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<OrderStartCheckVO> voList = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public OrderStartCheckVO getDetail(Long id) {
        OrderStartCheck entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(OrderStartCheckDTO dto) {
        OrderStartCheck entity = new OrderStartCheck();
        BeanUtils.copyProperties(dto, entity);
        entity.setChecker("system"); // TODO: SecurityContext
        entity.setCheckTime(LocalDateTime.now());
        save(entity);

        log.info("新增工单开工检查: workOrderId={}, checkItem={}, status={}",
                entity.getWorkOrderId(), entity.getCheckItem(), entity.getCheckStatus());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, OrderStartCheckDTO dto) {
        OrderStartCheck existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);

        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        existing.setCheckTime(LocalDateTime.now());
        updateById(existing);

        log.info("修改工单开工检查: id={}", id);
    }

    private OrderStartCheckVO toVO(OrderStartCheck entity) {
        OrderStartCheckVO vo = new OrderStartCheckVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
