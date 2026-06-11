package com.mes.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.plan.domain.dto.OrderPlanDTO;
import com.mes.plan.domain.dto.ProductionPlanDTO;
import com.mes.plan.domain.entity.OrderPlan;
import com.mes.plan.domain.query.OrderPlanQuery;
import com.mes.plan.domain.vo.OrderPlanVO;
import com.mes.plan.enums.*;
import com.mes.plan.mapper.OrderPlanMapper;
import com.mes.plan.service.IOrderPlanService;
import com.mes.plan.service.IPlanStatusLogService;
import com.mes.plan.service.IProductionPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 订单计划 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPlanServiceImpl extends ServiceImpl<OrderPlanMapper, OrderPlan>
        implements IOrderPlanService {

    private final IPlanStatusLogService planStatusLogService;
    private final ObjectProvider<IProductionPlanService> productionPlanServiceProvider;

    @Override
    public PageResult<OrderPlanVO> page(OrderPlanQuery query) {
        String businessType = resolveBusinessType(query.getBusinessType(), query.getWorkType());
        LambdaQueryWrapper<OrderPlan> wrapper = new LambdaQueryWrapper<OrderPlan>()
                .like(StringUtils.hasText(query.getOrderNo()),
                        OrderPlan::getOrderNo, query.getOrderNo())
                .like(StringUtils.hasText(query.getProductCode()),
                        OrderPlan::getProductCode, query.getProductCode())
                .like(StringUtils.hasText(query.getProductName()),
                        OrderPlan::getProductName, query.getProductName())
                .eq(StringUtils.hasText(query.getStatus()),
                        OrderPlan::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getFlowStatus()),
                        OrderPlan::getFlowStatus, query.getFlowStatus())
                .eq(StringUtils.hasText(query.getExpandStatus()),
                        OrderPlan::getExpandStatus, query.getExpandStatus())
                .eq(StringUtils.hasText(businessType),
                        OrderPlan::getBusinessType, businessType)
                .eq(StringUtils.hasText(query.getMachineModel()),
                        OrderPlan::getMachineModel, query.getMachineModel())
                .eq(StringUtils.hasText(query.getProductCategory()),
                        OrderPlan::getProductCategory, query.getProductCategory())
                .eq(StringUtils.hasText(query.getDataSource()),
                        OrderPlan::getDataSource, query.getDataSource())
                .orderByDesc(OrderPlan::getCreatedTime);

        Page<OrderPlan> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<OrderPlanVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public OrderPlanVO getDetail(Long id) {
        OrderPlan entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(OrderPlanDTO dto) {
        // 校验订单号唯一性
        long count = count(new LambdaQueryWrapper<OrderPlan>()
                .eq(OrderPlan::getOrderNo, dto.getOrderNo()));
        AssertUtil.isFalse(count > 0, "订单号 " + dto.getOrderNo() + " 已存在");

        OrderPlan entity = new OrderPlan();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(OrderPlanStatus.CREATED.getCode());
        entity.setFlowStatus(FlowStatus.RUNNING.getCode());
        entity.setExpandStatus(ExpandStatus.UNEXPANDED.getCode());
        entity.setCompletionStatus(CompletionStatus.NOT_STARTED.getCode());
        if (!StringUtils.hasText(entity.getDataSource())) {
            entity.setDataSource("MANUAL");
        }
        save(entity);

        planStatusLogService.log(PlanType.ORDER.getCode(), entity.getId(),
                null, OrderPlanStatus.CREATED.getCode(), "创建",
                "创建订单计划 " + entity.getOrderNo());

        log.info("新增订单计划: {}", entity.getOrderNo());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, OrderPlanDTO dto) {
        OrderPlan existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);

        // 仅创建状态可编辑
        AssertUtil.isTrue(OrderPlanStatus.CREATED.getCode().equals(existing.getStatus()),
                "仅创建状态的订单计划可以编辑");

        String status = existing.getStatus();
        String flowStatus = existing.getFlowStatus();
        String expandStatus = existing.getExpandStatus();
        String completionStatus = existing.getCompletionStatus();

        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        existing.setStatus(status);
        existing.setFlowStatus(flowStatus);
        existing.setExpandStatus(expandStatus);
        existing.setCompletionStatus(completionStatus);
        updateById(existing);

        log.info("修改订单计划: {}", existing.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        OrderPlan entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        // 仅创建状态可删除
        AssertUtil.isTrue(OrderPlanStatus.CREATED.getCode().equals(entity.getStatus()),
                "仅创建状态的订单计划可以删除");

        removeById(id);
        log.info("删除订单计划: {}", entity.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(Long id) {
        OrderPlan entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(OrderPlanStatus.CREATED.getCode().equals(entity.getStatus()),
                "仅创建状态的订单计划可以下达");

        String fromStatus = entity.getStatus();
        entity.setStatus(OrderPlanStatus.RELEASED.getCode());
        updateById(entity);

        planStatusLogService.log(PlanType.ORDER.getCode(), id,
                fromStatus, OrderPlanStatus.RELEASED.getCode(), "下达",
                "订单计划 " + entity.getOrderNo() + " 下达");

        log.info("订单计划下达: {}", entity.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id) {
        OrderPlan entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(OrderPlanStatus.RELEASED.getCode().equals(entity.getStatus()),
                "仅已下达状态的订单计划可以完成");

        String fromStatus = entity.getStatus();
        entity.setStatus(OrderPlanStatus.COMPLETED.getCode());
        entity.setFlowStatus(FlowStatus.COMPLETED.getCode());
        entity.setCompletionStatus(CompletionStatus.APPROVED.getCode());
        updateById(entity);

        planStatusLogService.log(PlanType.ORDER.getCode(), id,
                fromStatus, OrderPlanStatus.COMPLETED.getCode(), "完成",
                "订单计划 " + entity.getOrderNo() + " 完成");

        log.info("订单计划完成: {}", entity.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminate(Long id, String reason) {
        OrderPlan entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(OrderPlanStatus.RELEASED.getCode().equals(entity.getStatus()),
                "仅已下达状态的订单计划可以终止");

        String fromStatus = entity.getStatus();
        entity.setStatus(OrderPlanStatus.TERMINATED.getCode());
        entity.setFlowStatus(FlowStatus.TERMINATED.getCode());
        updateById(entity);

        planStatusLogService.log(PlanType.ORDER.getCode(), id,
                fromStatus, OrderPlanStatus.TERMINATED.getCode(), "终止",
                "订单计划 " + entity.getOrderNo() + " 终止，原因: " + reason);

        log.info("订单计划终止: {}, 原因: {}", entity.getOrderNo(), reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expand(Long id) {
        OrderPlan entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(ExpandStatus.UNEXPANDED.getCode().equals(entity.getExpandStatus()),
                "该订单计划已经展开");
        AssertUtil.isTrue(OrderPlanStatus.RELEASED.getCode().equals(entity.getStatus()),
                "仅已下达状态的订单计划可以展开");

        Long productionPlanId = productionPlanServiceProvider.getObject()
                .create(buildProductionPlan(entity));

        entity.setExpandStatus(ExpandStatus.EXPANDED.getCode());
        updateById(entity);

        planStatusLogService.log(PlanType.ORDER.getCode(), id,
                ExpandStatus.UNEXPANDED.getCode(), ExpandStatus.EXPANDED.getCode(), "展开",
                "订单计划 " + entity.getOrderNo() + " 展开为生产计划，生产计划ID=" + productionPlanId);

        log.info("订单计划展开: {}, 生产计划ID={}", entity.getOrderNo(), productionPlanId);
    }

    private OrderPlanVO toVO(OrderPlan entity) {
        OrderPlanVO vo = new OrderPlanVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setWorkType(entity.getBusinessType());
        return vo;
    }

    private String resolveBusinessType(String businessType, String legacyWorkType) {
        return StringUtils.hasText(businessType) ? businessType : legacyWorkType;
    }

    private ProductionPlanDTO buildProductionPlan(OrderPlan entity) {
        ProductionPlanDTO dto = new ProductionPlanDTO();
        dto.setOrderPlanId(entity.getId());
        dto.setOrderNo(entity.getOrderNo());
        dto.setProductCode(entity.getProductCode());
        dto.setProductName(entity.getProductName());
        dto.setNewOrRepairType(entity.getNewOrRepairType());
        dto.setBusinessType(entity.getBusinessType());
        dto.setMachineModel(entity.getMachineModel());
        dto.setProductCategory(entity.getProductCategory());
        dto.setProductType(entity.getProductType());
        dto.setWbsElement(entity.getWbsElement());
        dto.setPlanOrg(entity.getPlanOrg());
        dto.setPlanQty(entity.getPlanQty());
        dto.setQtyUnit(entity.getQtyUnit());
        dto.setPlanStartTime(entity.getPlanStartTime());
        dto.setPlanEndTime(entity.getPlanEndTime());
        return dto;
    }
}
