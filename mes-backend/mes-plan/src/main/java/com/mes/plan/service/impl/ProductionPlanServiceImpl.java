package com.mes.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.plan.domain.dto.ProductionPlanDTO;
import com.mes.plan.domain.entity.OrderPlan;
import com.mes.plan.domain.entity.ProductionPlan;
import com.mes.plan.domain.query.ProductionPlanQuery;
import com.mes.plan.domain.vo.ProductionPlanVO;
import com.mes.plan.enums.OrderPlanStatus;
import com.mes.plan.enums.PlanType;
import com.mes.plan.enums.ProductionPlanStatus;
import com.mes.plan.mapper.ProductionPlanMapper;
import com.mes.plan.service.IOrderPlanService;
import com.mes.plan.service.IPlanStatusLogService;
import com.mes.plan.service.IProductionPlanService;
import com.mes.workorder.domain.dto.WorkOrderDTO;
import com.mes.workorder.service.IWorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 生产计划 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionPlanServiceImpl extends ServiceImpl<ProductionPlanMapper, ProductionPlan>
        implements IProductionPlanService {

    private final IOrderPlanService orderPlanService;
    private final IPlanStatusLogService planStatusLogService;
    private final IWorkOrderService workOrderService;

    @Override
    public PageResult<ProductionPlanVO> page(ProductionPlanQuery query) {
        LambdaQueryWrapper<ProductionPlan> wrapper = new LambdaQueryWrapper<ProductionPlan>()
                .like(StringUtils.hasText(query.getOrderNo()),
                        ProductionPlan::getOrderNo, query.getOrderNo())
                .like(StringUtils.hasText(query.getProductCode()),
                        ProductionPlan::getProductCode, query.getProductCode())
                .like(StringUtils.hasText(query.getProductName()),
                        ProductionPlan::getProductName, query.getProductName())
                .eq(StringUtils.hasText(query.getStatus()),
                        ProductionPlan::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getWorkType()),
                        ProductionPlan::getWorkType, query.getWorkType())
                .eq(StringUtils.hasText(query.getMachineModel()),
                        ProductionPlan::getMachineModel, query.getMachineModel())
                .eq(StringUtils.hasText(query.getProductCategory()),
                        ProductionPlan::getProductCategory, query.getProductCategory())
                .eq(query.getOrderPlanId() != null,
                        ProductionPlan::getOrderPlanId, query.getOrderPlanId())
                .orderByDesc(ProductionPlan::getCreatedTime);

        Page<ProductionPlan> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<ProductionPlanVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public ProductionPlanVO getDetail(Long id) {
        ProductionPlan entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProductionPlanDTO dto) {
        // 校验订单计划存在且状态正确
        OrderPlan orderPlan = orderPlanService.getById(dto.getOrderPlanId());
        AssertUtil.notNull(orderPlan, "关联的订单计划不存在");
        AssertUtil.isTrue(OrderPlanStatus.RELEASED.getCode().equals(orderPlan.getStatus()),
                "关联的订单计划必须为已下达状态");

        // 校验计划数量
        AssertUtil.isTrue(dto.getPlanQty() != null && dto.getPlanQty().compareTo(BigDecimal.ZERO) > 0,
                "计划数量必须大于0");

        ProductionPlan entity = new ProductionPlan();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(ProductionPlanStatus.CREATED.getCode());
        entity.setCompletedQty(BigDecimal.ZERO);
        // 从订单计划复制订单编号
        if (!StringUtils.hasText(entity.getOrderNo())) {
            entity.setOrderNo(orderPlan.getOrderNo());
        }
        save(entity);

        planStatusLogService.log(PlanType.PRODUCTION.getCode(), entity.getId(),
                null, ProductionPlanStatus.CREATED.getCode(), "创建",
                "创建生产计划，关联订单 " + orderPlan.getOrderNo());

        log.info("新增生产计划: 订单计划={}", orderPlan.getOrderNo());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductionPlanDTO dto) {
        ProductionPlan existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(ProductionPlanStatus.CREATED.getCode().equals(existing.getStatus()),
                "仅创建状态的生产计划可以编辑");

        String status = existing.getStatus();
        BigDecimal completedQty = existing.getCompletedQty();

        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        existing.setStatus(status);
        existing.setCompletedQty(completedQty);
        updateById(existing);

        log.info("修改生产计划: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProductionPlan entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(ProductionPlanStatus.CREATED.getCode().equals(entity.getStatus()),
                "仅创建状态的生产计划可以删除");

        removeById(id);
        log.info("删除生产计划: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(Long id) {
        ProductionPlan entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(ProductionPlanStatus.CREATED.getCode().equals(entity.getStatus()),
                "仅创建状态的生产计划可以下达");

        String fromStatus = entity.getStatus();
        entity.setStatus(ProductionPlanStatus.RELEASED.getCode());
        updateById(entity);

        planStatusLogService.log(PlanType.PRODUCTION.getCode(), id,
                fromStatus, ProductionPlanStatus.RELEASED.getCode(), "下达",
                "生产计划下达");

        // 自动创建工单
        OrderPlan orderPlan = orderPlanService.getById(entity.getOrderPlanId());
        String workOrderNo = generateWorkOrderNo();

        WorkOrderDTO workOrderDTO = new WorkOrderDTO();
        workOrderDTO.setWorkOrderNo(workOrderNo);
        workOrderDTO.setWorkOrderType(entity.getWorkOrderType());
        workOrderDTO.setProductionPlanNo(String.valueOf(entity.getId()));
        workOrderDTO.setOrderPlanNo(orderPlan != null ? orderPlan.getOrderNo() : null);
        workOrderDTO.setOrderNo(entity.getOrderNo());
        workOrderDTO.setProductCode(entity.getProductCode());
        workOrderDTO.setProductName(entity.getProductName());
        workOrderDTO.setMachineModel(entity.getMachineModel());
        workOrderDTO.setProductCategory(entity.getProductCategory());
        workOrderDTO.setProductType(entity.getProductType());
        workOrderDTO.setNewOrRepairType(entity.getNewOrRepairType());
        workOrderDTO.setWorkType(entity.getWorkType());
        workOrderDTO.setPlanQty(entity.getPlanQty());
        workOrderDTO.setQtyUnit(entity.getQtyUnit());
        workOrderDTO.setPlanOrg(entity.getPlanOrg());
        workOrderDTO.setWbsElement(entity.getWbsElement());
        workOrderDTO.setPlanStartTime(entity.getPlanStartTime());
        workOrderDTO.setPlanEndTime(entity.getPlanEndTime());

        Long workOrderId = workOrderService.create(workOrderDTO);

        log.info("生产计划下达: id={}, 自动创建工单: {} (id={})", id, workOrderNo, workOrderId);
    }

    /**
     * 生成工单号：WO-yyyyMMdd-XXXX
     */
    private String generateWorkOrderNo() {
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seq = String.format("%04d", (int) (Math.random() * 9999) + 1);
        return "WO-" + dateStr + "-" + seq;
    }

    private ProductionPlanVO toVO(ProductionPlan entity) {
        ProductionPlanVO vo = new ProductionPlanVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
