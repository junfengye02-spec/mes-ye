package com.mes.plan.listener;

import com.mes.common.event.WorkOrderCompletedEvent;
import com.mes.plan.domain.entity.OrderPlan;
import com.mes.plan.domain.entity.ProductionPlan;
import com.mes.plan.enums.OrderPlanStatus;
import com.mes.plan.enums.PlanType;
import com.mes.plan.enums.ProductionPlanStatus;
import com.mes.plan.mapper.ProductionPlanMapper;
import com.mes.plan.service.IOrderPlanService;
import com.mes.plan.service.IPlanStatusLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionPlanEventListenerTest {

    @Mock
    private ProductionPlanMapper productionPlanMapper;
    @Mock
    private IPlanStatusLogService planStatusLogService;
    @Mock
    private IOrderPlanService orderPlanService;

    @InjectMocks
    private ProductionPlanEventListener listener;

    @Test
    @DisplayName("工单完工事件更新生产计划并级联完成订单计划")
    void onWorkOrderCompleted_updatesPlanAndCompletesOrderPlan() {
        ProductionPlan plan = new ProductionPlan();
        plan.setId(100L);
        plan.setOrderPlanId(10L);
        plan.setPlanQty(new BigDecimal("5"));
        plan.setCompletedQty(BigDecimal.ZERO);
        plan.setStatus(ProductionPlanStatus.RELEASED.getCode());

        when(productionPlanMapper.selectById(100L)).thenReturn(plan);
        when(productionPlanMapper.updateById(any(ProductionPlan.class))).thenReturn(1);
        when(productionPlanMapper.selectCount(any())).thenReturn(0L);
        when(orderPlanService.getById(10L)).thenReturn(orderPlan(10L, OrderPlanStatus.RELEASED));

        listener.onWorkOrderCompleted(new WorkOrderCompletedEvent(
                this, 1L, "WO-001", "100", "ORDPLAN-001",
                new BigDecimal("5"), LocalDateTime.of(2026, 5, 27, 13, 0)));

        ArgumentCaptor<ProductionPlan> captor = ArgumentCaptor.forClass(ProductionPlan.class);
        verify(productionPlanMapper).updateById(captor.capture());
        ProductionPlan updated = captor.getValue();
        assertEquals(ProductionPlanStatus.COMPLETED.getCode(), updated.getStatus());
        assertEquals(new BigDecimal("5"), updated.getCompletedQty());
        assertNotNull(updated.getActualEndTime());
        verify(planStatusLogService).log(eq(PlanType.PRODUCTION.getCode()), eq(100L),
                eq(ProductionPlanStatus.RELEASED.getCode()),
                eq(ProductionPlanStatus.COMPLETED.getCode()), eq("完成"), any());
        verify(orderPlanService).complete(10L);
    }

    @Test
    @DisplayName("同一订单仍有未完工生产计划时，不提前自动完成订单计划")
    void onWorkOrderCompleted_keepsOrderPlanReleasedWhenSiblingPlansRemain() {
        ProductionPlan plan = new ProductionPlan();
        plan.setId(100L);
        plan.setOrderPlanId(10L);
        plan.setPlanQty(new BigDecimal("5"));
        plan.setCompletedQty(BigDecimal.ZERO);
        plan.setStatus(ProductionPlanStatus.RELEASED.getCode());

        when(productionPlanMapper.selectById(100L)).thenReturn(plan);
        when(productionPlanMapper.updateById(any(ProductionPlan.class))).thenReturn(1);
        when(productionPlanMapper.selectCount(any())).thenReturn(1L);

        listener.onWorkOrderCompleted(new WorkOrderCompletedEvent(
                this, 1L, "WO-001", "100", "ORDPLAN-001",
                new BigDecimal("5"), LocalDateTime.of(2026, 5, 27, 13, 0)));

        verify(productionPlanMapper).updateById(any(ProductionPlan.class));
        verify(orderPlanService, never()).complete(10L);
    }

    private static OrderPlan orderPlan(Long id, OrderPlanStatus status) {
        OrderPlan orderPlan = new OrderPlan();
        orderPlan.setId(id);
        orderPlan.setStatus(status.getCode());
        return orderPlan;
    }
}
