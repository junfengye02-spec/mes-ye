package com.mes.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.id.DistributedIdGenerator;
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
import com.mes.process.domain.vo.RouteStepVO;
import com.mes.process.domain.vo.RouteVO;
import com.mes.process.service.IRouteService;
import com.mes.workorder.domain.dto.WorkOrderDTO;
import com.mes.workorder.domain.dto.WorkOrderTaskDTO;
import com.mes.workorder.service.IWorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * 生产计划 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionPlanServiceImpl extends ServiceImpl<ProductionPlanMapper, ProductionPlan>
        implements IProductionPlanService {

    private static final DateTimeFormatter WORK_ORDER_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final IOrderPlanService orderPlanService;
    private final IPlanStatusLogService planStatusLogService;
    private final IWorkOrderService workOrderService;
    private final IRouteService routeService;
    private final DistributedIdGenerator distributedIdGenerator;

    @Override
    public PageResult<ProductionPlanVO> page(ProductionPlanQuery query) {
        String businessType = resolveBusinessType(query.getBusinessType(), query.getWorkType());
        LambdaQueryWrapper<ProductionPlan> wrapper = new LambdaQueryWrapper<ProductionPlan>()
                .like(StringUtils.hasText(query.getOrderNo()),
                        ProductionPlan::getOrderNo, query.getOrderNo())
                .like(StringUtils.hasText(query.getProductCode()),
                        ProductionPlan::getProductCode, query.getProductCode())
                .like(StringUtils.hasText(query.getProductName()),
                        ProductionPlan::getProductName, query.getProductName())
                .eq(StringUtils.hasText(query.getStatus()),
                        ProductionPlan::getStatus, query.getStatus())
                .eq(StringUtils.hasText(businessType),
                        ProductionPlan::getBusinessType, businessType)
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

        List<WorkOrderTaskDTO> tasks = buildWorkOrderTasks(entity);

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
        workOrderDTO.setBusinessType(entity.getBusinessType());
        workOrderDTO.setPlanQty(entity.getPlanQty());
        workOrderDTO.setQtyUnit(entity.getQtyUnit());
        workOrderDTO.setPlanOrg(entity.getPlanOrg());
        workOrderDTO.setWbsElement(entity.getWbsElement());
        workOrderDTO.setPlanStartTime(entity.getPlanStartTime());
        workOrderDTO.setPlanEndTime(entity.getPlanEndTime());
        workOrderDTO.setTasks(tasks);

        Long workOrderId = workOrderService.create(workOrderDTO);

        log.info("生产计划下达: id={}, 自动创建工单: {} (id={})", id, workOrderNo, workOrderId);
    }

    private List<WorkOrderTaskDTO> buildWorkOrderTasks(ProductionPlan plan) {
        RouteVO route = routeService.findActiveRouteWithSteps(
                plan.getProductCode(), plan.getProductCategory(),
                plan.getMachineModel(), plan.getProductType());
        AssertUtil.isTrue(route.getSteps() != null && !route.getSteps().isEmpty(),
                "工艺路线未配置工序步骤");

        return route.getSteps().stream()
                .sorted(Comparator
                        .comparing(RouteStepVO::getSequenceNo,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RouteStepVO::getId,
                                Comparator.nullsLast(Long::compareTo)))
                .map(step -> {
                    WorkOrderTaskDTO dto = new WorkOrderTaskDTO();
                    dto.setTaskNo(step.getProcessNo());
                    dto.setTaskName(step.getProcessName());
                    dto.setPlanWorkCenterId(step.getWorkCenterId());
                    dto.setPlanQty(plan.getPlanQty());
                    dto.setQtyUnit(plan.getQtyUnit());
                    dto.setSequenceNo(step.getSequenceNo());
                    return dto;
                })
                .toList();
    }

    /**
     * 生成工单号：WO-yyyyMMdd-{distributedId}
     */
    private String generateWorkOrderNo() {
        String dateStr = LocalDate.now().format(WORK_ORDER_DATE_FMT);
        return "WO-" + dateStr + "-" + distributedIdGenerator.nextIdStr();
    }

    private ProductionPlanVO toVO(ProductionPlan entity) {
        ProductionPlanVO vo = new ProductionPlanVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setWorkType(entity.getBusinessType());
        return vo;
    }

    private String resolveBusinessType(String businessType, String legacyWorkType) {
        return StringUtils.hasText(businessType) ? businessType : legacyWorkType;
    }
}
