package com.mes.quality.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.quality.domain.dto.RecheckOrderPlanDTO;
import com.mes.quality.domain.dto.RecheckRequestDTO;
import com.mes.quality.domain.dto.RecheckSerialDTO;
import com.mes.quality.domain.entity.RecheckOrderPlan;
import com.mes.quality.domain.entity.RecheckRequest;
import com.mes.quality.domain.entity.RecheckSerial;
import com.mes.quality.domain.query.RecheckRequestQuery;
import com.mes.quality.domain.vo.RecheckOrderPlanVO;
import com.mes.quality.domain.vo.RecheckRequestVO;
import com.mes.quality.domain.vo.RecheckSerialVO;
import com.mes.quality.enums.RecheckStatus;
import com.mes.quality.mapper.RecheckOrderPlanMapper;
import com.mes.quality.mapper.RecheckRequestMapper;
import com.mes.quality.mapper.RecheckSerialMapper;
import com.mes.quality.service.IRecheckRequestService;
import com.mes.common.event.ApsSyncEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecheckRequestServiceImpl extends ServiceImpl<RecheckRequestMapper, RecheckRequest>
        implements IRecheckRequestService {

    private final RecheckOrderPlanMapper orderPlanMapper;
    private final RecheckSerialMapper serialMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PageResult<RecheckRequestVO> page(RecheckRequestQuery query) {
        LambdaQueryWrapper<RecheckRequest> wrapper = new LambdaQueryWrapper<RecheckRequest>()
                .like(StringUtils.hasText(query.getProjectCode()), RecheckRequest::getProjectCode, query.getProjectCode())
                .like(StringUtils.hasText(query.getMaterialCode()), RecheckRequest::getMaterialCode, query.getMaterialCode())
                .like(StringUtils.hasText(query.getProductionOrderNo()), RecheckRequest::getProductionOrderNo, query.getProductionOrderNo())
                .eq(StringUtils.hasText(query.getStatus()), RecheckRequest::getStatus, query.getStatus())
                .orderByDesc(RecheckRequest::getCreatedTime);

        Page<RecheckRequest> page = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<RecheckRequestVO> voList = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public RecheckRequestVO getDetail(Long id) {
        RecheckRequest entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        RecheckRequestVO vo = toVO(entity);

        // 查询关联订单计划
        List<RecheckOrderPlan> plans = orderPlanMapper.selectList(
                new LambdaQueryWrapper<RecheckOrderPlan>().eq(RecheckOrderPlan::getRecheckId, id));
        vo.setOrderPlans(plans.stream().map(p -> {
            RecheckOrderPlanVO pv = new RecheckOrderPlanVO();
            BeanUtils.copyProperties(p, pv);
            return pv;
        }).toList());

        // 查询序列号
        List<RecheckSerial> serials = serialMapper.selectList(
                new LambdaQueryWrapper<RecheckSerial>().eq(RecheckSerial::getRecheckId, id));
        vo.setSerials(serials.stream().map(s -> {
            RecheckSerialVO sv = new RecheckSerialVO();
            BeanUtils.copyProperties(s, sv);
            return sv;
        }).toList());

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RecheckRequestDTO dto) {
        RecheckRequest entity = new RecheckRequest();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(RecheckStatus.CREATED.getCode());
        save(entity);

        Long recheckId = entity.getId();

        // 保存关联订单计划
        if (!CollectionUtils.isEmpty(dto.getOrderPlans())) {
            for (RecheckOrderPlanDTO planDTO : dto.getOrderPlans()) {
                RecheckOrderPlan plan = new RecheckOrderPlan();
                BeanUtils.copyProperties(planDTO, plan);
                plan.setRecheckId(recheckId);
                plan.setCreatedTime(LocalDateTime.now());
                plan.setUpdatedTime(LocalDateTime.now());
                orderPlanMapper.insert(plan);
            }
        }

        // 保存序列号
        if (!CollectionUtils.isEmpty(dto.getSerials())) {
            for (RecheckSerialDTO serialDTO : dto.getSerials()) {
                RecheckSerial serial = new RecheckSerial();
                BeanUtils.copyProperties(serialDTO, serial);
                serial.setRecheckId(recheckId);
                serialMapper.insert(serial);
            }
        }

        // 发布APS同步事件：质量数据
        try {
            String payload = String.format(
                    "{\"recheckId\":%d,\"materialCode\":\"%s\",\"status\":\"%s\"}",
                    recheckId,
                    entity.getMaterialCode() != null ? entity.getMaterialCode() : "",
                    entity.getStatus());
            eventPublisher.publishEvent(new ApsSyncEvent(
                    this, "QUALITY", "RECHECK",
                    recheckId, entity.getMaterialCode(), 5, payload));
        } catch (Exception e) {
            log.warn("发布质量APS同步事件失败（不影响业务）: {}", e.getMessage());
        }

        log.info("新增复检申请: id={}", recheckId);
        return recheckId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RecheckRequestDTO dto) {
        RecheckRequest existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);

        String status = existing.getStatus();
        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        existing.setStatus(status);
        updateById(existing);

        // 先删后插更新子表
        orderPlanMapper.delete(new LambdaQueryWrapper<RecheckOrderPlan>().eq(RecheckOrderPlan::getRecheckId, id));
        serialMapper.delete(new LambdaQueryWrapper<RecheckSerial>().eq(RecheckSerial::getRecheckId, id));

        if (!CollectionUtils.isEmpty(dto.getOrderPlans())) {
            for (RecheckOrderPlanDTO planDTO : dto.getOrderPlans()) {
                RecheckOrderPlan plan = new RecheckOrderPlan();
                BeanUtils.copyProperties(planDTO, plan);
                plan.setRecheckId(id);
                plan.setCreatedTime(LocalDateTime.now());
                plan.setUpdatedTime(LocalDateTime.now());
                orderPlanMapper.insert(plan);
            }
        }

        if (!CollectionUtils.isEmpty(dto.getSerials())) {
            for (RecheckSerialDTO serialDTO : dto.getSerials()) {
                RecheckSerial serial = new RecheckSerial();
                BeanUtils.copyProperties(serialDTO, serial);
                serial.setRecheckId(id);
                serialMapper.insert(serial);
            }
        }

        log.info("修改复检申请: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        RecheckRequest entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        orderPlanMapper.delete(new LambdaQueryWrapper<RecheckOrderPlan>().eq(RecheckOrderPlan::getRecheckId, id));
        serialMapper.delete(new LambdaQueryWrapper<RecheckSerial>().eq(RecheckSerial::getRecheckId, id));
        removeById(id);

        log.info("删除复检申请: id={}", id);
    }

    private RecheckRequestVO toVO(RecheckRequest entity) {
        RecheckRequestVO vo = new RecheckRequestVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
