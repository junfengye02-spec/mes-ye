package com.mes.plan;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.plan.domain.dto.OrderPlanDTO;
import com.mes.plan.domain.dto.ProductionPlanDTO;
import com.mes.plan.domain.entity.OrderPlan;
import com.mes.plan.enums.*;
import com.mes.plan.mapper.OrderPlanMapper;
import com.mes.plan.service.IProductionPlanService;
import com.mes.plan.service.IPlanStatusLogService;
import com.mes.plan.service.impl.OrderPlanServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 计划管理模块单元测试
 * 覆盖订单计划的完整生命周期（创建→下达→完成/终止/展开）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlanModuleTest {

    @Mock private OrderPlanMapper orderPlanMapper;
    @Mock private IPlanStatusLogService planStatusLogService;
    @Mock private ObjectProvider<IProductionPlanService> productionPlanServiceProvider;
    @Mock private IProductionPlanService productionPlanService;

    @Spy
    @InjectMocks
    private OrderPlanServiceImpl orderPlanService;

    @BeforeEach
    void bindBaseMapper() {
        // MyBatis-Plus ServiceImpl 的 baseMapper 来自父类字段，Mockito 不会自动注入，需要反射绑定
        ReflectionTestUtils.setField(orderPlanService, "baseMapper", orderPlanMapper);
    }

    // ==================== 1. 订单计划创建测试 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 创建订单计划 - 正常流程")
    void testCreateOrderPlan_Success() {
        OrderPlanDTO dto = buildOrderPlanDTO("ORD-2024-001");

        when(orderPlanMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orderPlanMapper.insert(any(OrderPlan.class))).thenReturn(1);

        Long id = orderPlanService.create(dto);

        verify(orderPlanMapper).insert(argThat(plan -> {
            assertEquals(OrderPlanStatus.CREATED.getCode(), plan.getStatus(), "初始状态应为 CREATED");
            assertEquals(FlowStatus.RUNNING.getCode(), plan.getFlowStatus(), "初始流程状态应为 RUNNING");
            assertEquals(ExpandStatus.UNEXPANDED.getCode(), plan.getExpandStatus(), "初始展开状态应为 UNEXPANDED");
            assertEquals(CompletionStatus.NOT_STARTED.getCode(), plan.getCompletionStatus(), "初始完成状态应为 NOT_STARTED");
            assertEquals("MANUAL", plan.getDataSource(), "默认数据来源应为 MANUAL");
            return true;
        }));

        verify(planStatusLogService).log(eq(PlanType.ORDER.getCode()), any(),
                isNull(), eq(OrderPlanStatus.CREATED.getCode()), eq("创建"), anyString());
    }

    @Test
    @Order(2)
    @DisplayName("1.2 创建订单计划 - 订单号重复应拒绝")
    void testCreateOrderPlan_DuplicateOrderNo() {
        OrderPlanDTO dto = buildOrderPlanDTO("ORD-2024-001");

        when(orderPlanMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(Exception.class, () -> orderPlanService.create(dto),
                "重复的订单号应被拒绝");
    }

    // ==================== 2. 订单计划编辑/删除测试 ====================

    @Test
    @Order(10)
    @DisplayName("2.1 编辑 CREATED 状态的计划 - 应成功")
    void testUpdateOrderPlan_CreatedStatus() {
        OrderPlan existing = buildOrderPlan(1L, "ORD-2024-001", OrderPlanStatus.CREATED);
        OrderPlanDTO dto = buildOrderPlanDTO("ORD-2024-001");
        dto.setProductName("新产品名称");

        when(orderPlanMapper.selectById(1L)).thenReturn(existing);
        when(orderPlanMapper.updateById(any(OrderPlan.class))).thenReturn(1);

        orderPlanService.update(1L, dto);

        verify(orderPlanMapper).updateById(argThat(plan -> {
            assertEquals(OrderPlanStatus.CREATED.getCode(), plan.getStatus(), "状态不应被覆盖");
            return true;
        }));
    }

    @Test
    @Order(11)
    @DisplayName("2.2 编辑非 CREATED 状态的计划 - 应拒绝")
    void testUpdateOrderPlan_ReleasedStatus() {
        OrderPlan existing = buildOrderPlan(1L, "ORD-2024-001", OrderPlanStatus.RELEASED);

        when(orderPlanMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class,
                () -> orderPlanService.update(1L, buildOrderPlanDTO("ORD-2024-001")),
                "已下达的计划不应允许编辑");
    }

    @Test
    @Order(12)
    @DisplayName("2.3 删除 CREATED 状态的计划 - 应成功")
    void testDeleteOrderPlan_CreatedStatus() {
        OrderPlan existing = buildOrderPlan(1L, "ORD-2024-001", OrderPlanStatus.CREATED);

        when(orderPlanMapper.selectById(1L)).thenReturn(existing);
        // ServiceImpl.removeById 内部依赖 TableInfoHelper 注册表，单测环境下直接 Spy 打桩
        doReturn(true).when(orderPlanService).removeById(1L);

        orderPlanService.delete(1L);

        verify(orderPlanService).removeById(1L);
    }

    @Test
    @Order(13)
    @DisplayName("2.4 删除非 CREATED 状态的计划 - 应拒绝")
    void testDeleteOrderPlan_ReleasedStatus() {
        OrderPlan existing = buildOrderPlan(1L, "ORD-2024-001", OrderPlanStatus.RELEASED);

        when(orderPlanMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class, () -> orderPlanService.delete(1L),
                "已下达的计划不应允许删除");
    }

    // ==================== 3. 状态流转测试 ====================

    @Test
    @Order(20)
    @DisplayName("3.1 下达计划 - CREATED → RELEASED")
    void testRelease_Success() {
        OrderPlan existing = buildOrderPlan(1L, "ORD-2024-001", OrderPlanStatus.CREATED);

        when(orderPlanMapper.selectById(1L)).thenReturn(existing);
        when(orderPlanMapper.updateById(any(OrderPlan.class))).thenReturn(1);

        orderPlanService.release(1L);

        verify(orderPlanMapper).updateById(argThat(plan ->
                OrderPlanStatus.RELEASED.getCode().equals(plan.getStatus())));
        verify(planStatusLogService).log(eq(PlanType.ORDER.getCode()), eq(1L),
                eq(OrderPlanStatus.CREATED.getCode()), eq(OrderPlanStatus.RELEASED.getCode()),
                eq("下达"), anyString());
    }

    @Test
    @Order(21)
    @DisplayName("3.2 非 CREATED 状态下达 - 应拒绝")
    void testRelease_NotCreatedStatus() {
        OrderPlan existing = buildOrderPlan(1L, "ORD-2024-001", OrderPlanStatus.RELEASED);

        when(orderPlanMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class, () -> orderPlanService.release(1L),
                "已下达的计划不应再次下达");
    }

    @Test
    @Order(22)
    @DisplayName("3.3 完成计划 - RELEASED → COMPLETED")
    void testComplete_Success() {
        OrderPlan existing = buildOrderPlan(1L, "ORD-2024-001", OrderPlanStatus.RELEASED);

        when(orderPlanMapper.selectById(1L)).thenReturn(existing);
        when(orderPlanMapper.updateById(any(OrderPlan.class))).thenReturn(1);

        orderPlanService.complete(1L);

        verify(orderPlanMapper).updateById(argThat(plan -> {
            assertEquals(OrderPlanStatus.COMPLETED.getCode(), plan.getStatus());
            assertEquals(FlowStatus.COMPLETED.getCode(), plan.getFlowStatus());
            assertEquals(CompletionStatus.APPROVED.getCode(), plan.getCompletionStatus());
            return true;
        }));
    }

    @Test
    @Order(23)
    @DisplayName("3.4 CREATED 状态直接完成 - 应拒绝")
    void testComplete_CreatedStatus() {
        OrderPlan existing = buildOrderPlan(1L, "ORD-2024-001", OrderPlanStatus.CREATED);

        when(orderPlanMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class, () -> orderPlanService.complete(1L),
                "CREATED 状态不应直接完成，必须先下达");
    }

    @Test
    @Order(24)
    @DisplayName("3.5 终止计划 - RELEASED → TERMINATED")
    void testTerminate_Success() {
        OrderPlan existing = buildOrderPlan(1L, "ORD-2024-001", OrderPlanStatus.RELEASED);

        when(orderPlanMapper.selectById(1L)).thenReturn(existing);
        when(orderPlanMapper.updateById(any(OrderPlan.class))).thenReturn(1);

        orderPlanService.terminate(1L, "客户取消订单");

        verify(orderPlanMapper).updateById(argThat(plan -> {
            assertEquals(OrderPlanStatus.TERMINATED.getCode(), plan.getStatus());
            assertEquals(FlowStatus.TERMINATED.getCode(), plan.getFlowStatus());
            return true;
        }));
    }

    @Test
    @Order(25)
    @DisplayName("3.6 CREATED 状态直接终止 - 应拒绝")
    void testTerminate_CreatedStatus() {
        OrderPlan existing = buildOrderPlan(1L, "ORD-2024-001", OrderPlanStatus.CREATED);

        when(orderPlanMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class, () -> orderPlanService.terminate(1L, "test"),
                "CREATED 状态不应直接终止");
    }

    // ==================== 4. 展开测试 ====================

    @Test
    @Order(30)
    @DisplayName("4.1 展开计划 - RELEASED + UNEXPANDED → EXPANDED")
    void testExpand_Success() {
        OrderPlan existing = buildOrderPlan(1L, "ORD-2024-001", OrderPlanStatus.RELEASED);
        existing.setExpandStatus(ExpandStatus.UNEXPANDED.getCode());

        when(orderPlanMapper.selectById(1L)).thenReturn(existing);
        when(productionPlanServiceProvider.getObject()).thenReturn(productionPlanService);
        when(productionPlanService.create(any(ProductionPlanDTO.class))).thenReturn(300L);
        when(orderPlanMapper.updateById(any(OrderPlan.class))).thenReturn(1);

        orderPlanService.expand(1L);

        verify(orderPlanMapper).updateById(argThat(plan ->
                ExpandStatus.EXPANDED.getCode().equals(plan.getExpandStatus())));
    }

    @Test
    @Order(31)
    @DisplayName("4.2 已展开的计划再次展开 - 应拒绝")
    void testExpand_AlreadyExpanded() {
        OrderPlan existing = buildOrderPlan(1L, "ORD-2024-001", OrderPlanStatus.RELEASED);
        existing.setExpandStatus(ExpandStatus.EXPANDED.getCode());

        when(orderPlanMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class, () -> orderPlanService.expand(1L),
                "已展开的计划不应再次展开");
    }

    @Test
    @Order(32)
    @DisplayName("4.3 CREATED 状态展开 - 应拒绝（必须先下达）")
    void testExpand_CreatedStatus() {
        OrderPlan existing = buildOrderPlan(1L, "ORD-2024-001", OrderPlanStatus.CREATED);
        existing.setExpandStatus(ExpandStatus.UNEXPANDED.getCode());

        when(orderPlanMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class, () -> orderPlanService.expand(1L),
                "未下达的计划不应允许展开");
    }

    // ==================== 5. 状态流转枚举验证 ====================

    @Test
    @Order(40)
    @DisplayName("5.1 OrderPlanStatus 枚举完整性")
    void testOrderPlanStatusEnum() {
        assertNotNull(OrderPlanStatus.CREATED);
        assertNotNull(OrderPlanStatus.RELEASED);
        assertNotNull(OrderPlanStatus.COMPLETED);
        assertNotNull(OrderPlanStatus.TERMINATED);
    }

    @Test
    @Order(41)
    @DisplayName("5.2 FlowStatus 枚举完整性")
    void testFlowStatusEnum() {
        assertNotNull(FlowStatus.RUNNING);
        assertNotNull(FlowStatus.COMPLETED);
        assertNotNull(FlowStatus.TERMINATED);
    }

    @Test
    @Order(42)
    @DisplayName("5.3 ExpandStatus 枚举完整性")
    void testExpandStatusEnum() {
        assertNotNull(ExpandStatus.UNEXPANDED);
        assertNotNull(ExpandStatus.EXPANDED);
    }

    // ==================== 辅助方法 ====================

    private OrderPlanDTO buildOrderPlanDTO(String orderNo) {
        OrderPlanDTO dto = new OrderPlanDTO();
        dto.setOrderNo(orderNo);
        dto.setProductCode("PROD-001");
        dto.setProductName("CFM56叶片");
        dto.setPlanQty(new BigDecimal("100"));
        return dto;
    }

    private OrderPlan buildOrderPlan(Long id, String orderNo, OrderPlanStatus status) {
        OrderPlan plan = new OrderPlan();
        plan.setId(id);
        plan.setOrderNo(orderNo);
        plan.setStatus(status.getCode());
        plan.setFlowStatus(FlowStatus.RUNNING.getCode());
        plan.setExpandStatus(ExpandStatus.UNEXPANDED.getCode());
        plan.setCompletionStatus(CompletionStatus.NOT_STARTED.getCode());
        return plan;
    }
}
