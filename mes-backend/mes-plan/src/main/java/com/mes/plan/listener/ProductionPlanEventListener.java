package com.mes.plan.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.common.event.WorkOrderCompletedEvent;
import com.mes.plan.domain.entity.OrderPlan;
import com.mes.plan.domain.entity.ProductionPlan;
import com.mes.plan.enums.OrderPlanStatus;
import com.mes.plan.enums.PlanType;
import com.mes.plan.enums.ProductionPlanStatus;
import com.mes.plan.mapper.ProductionPlanMapper;
import com.mes.plan.service.IOrderPlanService;
import com.mes.plan.service.IPlanStatusLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 工单事件驱动的生产计划级联监听器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductionPlanEventListener {

    private final ProductionPlanMapper productionPlanMapper;
    private final IPlanStatusLogService planStatusLogService;
    private final IOrderPlanService orderPlanService;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onWorkOrderCompleted(WorkOrderCompletedEvent event) {
        Long planId = parsePlanId(event.getProductionPlanNo());
        if (planId == null) {
            return;
        }

        ProductionPlan plan = productionPlanMapper.selectById(planId);
        if (plan == null) {
            log.warn("工单完工关联的生产计划不存在, productionPlanNo={}", event.getProductionPlanNo());
            return;
        }

        BigDecimal planQty = plan.getPlanQty() == null ? BigDecimal.ZERO : plan.getPlanQty();
        BigDecimal completedQty = plan.getCompletedQty() == null ? BigDecimal.ZERO : plan.getCompletedQty();
        BigDecimal increment = event.getCompletedQty() == null ? BigDecimal.ZERO : event.getCompletedQty();
        BigDecimal targetCompletedQty = completedQty.add(increment);
        if (planQty.compareTo(BigDecimal.ZERO) > 0 && targetCompletedQty.compareTo(planQty) > 0) {
            targetCompletedQty = planQty;
        }

        String fromStatus = plan.getStatus();
        plan.setCompletedQty(targetCompletedQty);
        if (plan.getActualStartTime() == null) {
            plan.setActualStartTime(event.getActualEndTime());
        }
        plan.setActualEndTime(event.getActualEndTime());

        boolean completed = planQty.compareTo(BigDecimal.ZERO) == 0
                || targetCompletedQty.compareTo(planQty) >= 0;
        if (completed) {
            plan.setStatus(ProductionPlanStatus.COMPLETED.getCode());
        }
        productionPlanMapper.updateById(plan);

        if (completed && !ProductionPlanStatus.COMPLETED.getCode().equals(fromStatus)) {
            planStatusLogService.log(PlanType.PRODUCTION.getCode(), plan.getId(),
                    fromStatus, ProductionPlanStatus.COMPLETED.getCode(), "完成",
                    "工单 " + event.getWorkOrderNo() + " 完工，生产计划自动完工");
        }

        if (completed && plan.getOrderPlanId() != null) {
            long remaining = productionPlanMapper.selectCount(new LambdaQueryWrapper<ProductionPlan>()
                    .eq(ProductionPlan::getOrderPlanId, plan.getOrderPlanId())
                    .ne(ProductionPlan::getStatus, ProductionPlanStatus.COMPLETED.getCode()));
            if (remaining == 0) {
                OrderPlan orderPlan = orderPlanService.getById(plan.getOrderPlanId());
                if (orderPlan != null && OrderPlanStatus.RELEASED.getCode().equals(orderPlan.getStatus())) {
                    orderPlanService.complete(plan.getOrderPlanId());
                }
            }
        }
    }

    private Long parsePlanId(String productionPlanNo) {
        if (!StringUtils.hasText(productionPlanNo)) {
            return null;
        }
        try {
            return Long.valueOf(productionPlanNo);
        } catch (NumberFormatException ex) {
            log.warn("productionPlanNo 不是数值，跳过自动级联: {}", productionPlanNo);
            return null;
        }
    }
}
