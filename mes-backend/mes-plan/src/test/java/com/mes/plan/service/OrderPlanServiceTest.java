package com.mes.plan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.common.exception.BusinessException;
import com.mes.plan.domain.dto.OrderPlanDTO;
import com.mes.plan.domain.entity.OrderPlan;
import com.mes.plan.enums.*;
import com.mes.plan.mapper.OrderPlanMapper;
import com.mes.plan.service.impl.OrderPlanServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link OrderPlanServiceImpl} 单元测试
 */
@ExtendWith(MockitoExtension.class)
class OrderPlanServiceTest {

    @Mock
    private OrderPlanMapper orderPlanMapper;
    @Mock
    private IPlanStatusLogService planStatusLogService;

    @InjectMocks
    private OrderPlanServiceImpl orderPlanService;

    @Test
    @DisplayName("创建订单计划 - 正常（初始状态 CREATED/RUNNING/UNEXPANDED/NOT_STARTED）")
    void create_success_initialFields() {
        OrderPlanDTO dto = baseDto("ORD-001");

        when(orderPlanMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orderPlanMapper.insert(any(OrderPlan.class))).thenAnswer(inv -> {
            OrderPlan p = inv.getArgument(0);
            p.setId(200L);
            return 1;
        });

        Long id = orderPlanService.create(dto);

        assertEquals(200L, id);
        verify(orderPlanMapper).insert(argThat(plan ->
                OrderPlanStatus.CREATED.getCode().equals(plan.getStatus())
                        && FlowStatus.RUNNING.getCode().equals(plan.getFlowStatus())
                        && ExpandStatus.UNEXPANDED.getCode().equals(plan.getExpandStatus())
                        && CompletionStatus.NOT_STARTED.getCode().equals(plan.getCompletionStatus())
                        && "MANUAL".equals(plan.getDataSource())));
        verify(planStatusLogService).log(eq(PlanType.ORDER.getCode()), eq(200L),
                isNull(), eq(OrderPlanStatus.CREATED.getCode()), eq("创建"), anyString());
    }

    @Test
    @DisplayName("创建订单计划 - 订单号重复")
    void create_duplicateOrderNo() {
        OrderPlanDTO dto = baseDto("ORD-DUP");
        when(orderPlanMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> orderPlanService.create(dto));
        assertTrue(ex.getMessage().contains("已存在"));
        verify(orderPlanMapper, never()).insert(any());
    }

    @Test
    @DisplayName("更新订单计划 - 仅 CREATED 状态可编辑")
    void update_success_onlyCreated() {
        OrderPlan existing = plan(1L, "ORD-1", OrderPlanStatus.CREATED);
        OrderPlanDTO dto = baseDto("ORD-1");
        dto.setProductName("新名称");

        when(orderPlanMapper.selectById(1L)).thenReturn(existing);
        when(orderPlanMapper.updateById(any(OrderPlan.class))).thenReturn(1);

        orderPlanService.update(1L, dto);

        verify(orderPlanMapper).updateById(argThat(p ->
                OrderPlanStatus.CREATED.getCode().equals(p.getStatus())));
    }

    @Test
    @DisplayName("更新订单计划 - 非 CREATED 不可编辑")
    void update_rejected_whenNotCreated() {
        OrderPlan existing = plan(1L, "ORD-1", OrderPlanStatus.RELEASED);
        when(orderPlanMapper.selectById(1L)).thenReturn(existing);

        assertThrows(BusinessException.class,
                () -> orderPlanService.update(1L, baseDto("ORD-1")));
        verify(orderPlanMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("删除订单计划 - 仅 CREATED 状态可删除")
    void delete_success_onlyCreated() {
        OrderPlan existing = plan(1L, "ORD-1", OrderPlanStatus.CREATED);
        when(orderPlanMapper.selectById(1L)).thenReturn(existing);
        when(orderPlanMapper.deleteById(1L)).thenReturn(1);

        orderPlanService.delete(1L);

        verify(orderPlanMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除订单计划 - 非 CREATED 不可删除")
    void delete_rejected_whenNotCreated() {
        OrderPlan existing = plan(1L, "ORD-1", OrderPlanStatus.RELEASED);
        when(orderPlanMapper.selectById(1L)).thenReturn(existing);

        assertThrows(BusinessException.class, () -> orderPlanService.delete(1L));
        verify(orderPlanMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("下达 - CREATED→RELEASED")
    void release_success() {
        OrderPlan existing = plan(1L, "ORD-1", OrderPlanStatus.CREATED);
        when(orderPlanMapper.selectById(1L)).thenReturn(existing);
        when(orderPlanMapper.updateById(any(OrderPlan.class))).thenReturn(1);

        orderPlanService.release(1L);

        verify(orderPlanMapper).updateById(argThat(p ->
                OrderPlanStatus.RELEASED.getCode().equals(p.getStatus())));
        verify(planStatusLogService).log(eq(PlanType.ORDER.getCode()), eq(1L),
                eq(OrderPlanStatus.CREATED.getCode()), eq(OrderPlanStatus.RELEASED.getCode()),
                eq("下达"), anyString());
    }

    @Test
    @DisplayName("下达 - 非 CREATED 不允许")
    void release_rejected_whenNotCreated() {
        OrderPlan existing = plan(1L, "ORD-1", OrderPlanStatus.RELEASED);
        when(orderPlanMapper.selectById(1L)).thenReturn(existing);

        assertThrows(BusinessException.class, () -> orderPlanService.release(1L));
    }

    @Test
    @DisplayName("完成 - RELEASED→COMPLETED（flowStatus=COMPLETED, completionStatus=APPROVED）")
    void complete_success() {
        OrderPlan existing = plan(1L, "ORD-1", OrderPlanStatus.RELEASED);
        when(orderPlanMapper.selectById(1L)).thenReturn(existing);
        when(orderPlanMapper.updateById(any(OrderPlan.class))).thenReturn(1);

        orderPlanService.complete(1L);

        verify(orderPlanMapper).updateById(argThat(p ->
                OrderPlanStatus.COMPLETED.getCode().equals(p.getStatus())
                        && FlowStatus.COMPLETED.getCode().equals(p.getFlowStatus())
                        && CompletionStatus.APPROVED.getCode().equals(p.getCompletionStatus())));
    }

    @Test
    @DisplayName("完成 - 非 RELEASED 不允许")
    void complete_rejected_whenNotReleased() {
        OrderPlan existing = plan(1L, "ORD-1", OrderPlanStatus.CREATED);
        when(orderPlanMapper.selectById(1L)).thenReturn(existing);

        assertThrows(BusinessException.class, () -> orderPlanService.complete(1L));
    }

    @Test
    @DisplayName("终止 - RELEASED→TERMINATED（flowStatus=TERMINATED）")
    void terminate_success() {
        OrderPlan existing = plan(1L, "ORD-1", OrderPlanStatus.RELEASED);
        when(orderPlanMapper.selectById(1L)).thenReturn(existing);
        when(orderPlanMapper.updateById(any(OrderPlan.class))).thenReturn(1);

        orderPlanService.terminate(1L, "客户取消");

        verify(orderPlanMapper).updateById(argThat(p ->
                OrderPlanStatus.TERMINATED.getCode().equals(p.getStatus())
                        && FlowStatus.TERMINATED.getCode().equals(p.getFlowStatus())));
    }

    @Test
    @DisplayName("展开 - RELEASED 且 UNEXPANDED→EXPANDED")
    void expand_success() {
        OrderPlan existing = plan(1L, "ORD-1", OrderPlanStatus.RELEASED);
        existing.setExpandStatus(ExpandStatus.UNEXPANDED.getCode());
        when(orderPlanMapper.selectById(1L)).thenReturn(existing);
        when(orderPlanMapper.updateById(any(OrderPlan.class))).thenReturn(1);

        orderPlanService.expand(1L);

        verify(orderPlanMapper).updateById(argThat(p ->
                ExpandStatus.EXPANDED.getCode().equals(p.getExpandStatus())));
    }

    @Test
    @DisplayName("展开 - 已展开不允许重复展开")
    void expand_rejected_whenAlreadyExpanded() {
        OrderPlan existing = plan(1L, "ORD-1", OrderPlanStatus.RELEASED);
        existing.setExpandStatus(ExpandStatus.EXPANDED.getCode());
        when(orderPlanMapper.selectById(1L)).thenReturn(existing);

        assertThrows(BusinessException.class, () -> orderPlanService.expand(1L));
        verify(orderPlanMapper, never()).updateById(any());
    }

    private static OrderPlanDTO baseDto(String orderNo) {
        OrderPlanDTO dto = new OrderPlanDTO();
        dto.setOrderNo(orderNo);
        dto.setProductCode("P1");
        dto.setProductName("产品");
        dto.setPlanQty(new BigDecimal("100"));
        return dto;
    }

    private static OrderPlan plan(Long id, String orderNo, OrderPlanStatus status) {
        OrderPlan p = new OrderPlan();
        p.setId(id);
        p.setOrderNo(orderNo);
        p.setStatus(status.getCode());
        p.setFlowStatus(FlowStatus.RUNNING.getCode());
        p.setExpandStatus(ExpandStatus.UNEXPANDED.getCode());
        p.setCompletionStatus(CompletionStatus.NOT_STARTED.getCode());
        return p;
    }
}
