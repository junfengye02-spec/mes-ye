package com.mes.dispatch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.common.exception.BusinessException;
import com.mes.dispatch.domain.dto.DispatchTaskCreateDTO;
import com.mes.dispatch.domain.entity.DispatchTask;
import com.mes.dispatch.enums.DispatchStatus;
import com.mes.dispatch.mapper.DispatchAssignmentMapper;
import com.mes.dispatch.mapper.DispatchTaskMapper;
import com.mes.dispatch.service.impl.DispatchTaskServiceImpl;
import com.mes.framework.tenant.TenantContextHolder;
import com.mes.workorder.domain.entity.WorkOrder;
import com.mes.workorder.domain.entity.WorkOrderTask;
import com.mes.workorder.mapper.WorkOrderMapper;
import com.mes.workorder.mapper.WorkOrderTaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0 修复 R3（mcp30）：派工任务租户隔离回归测试。
 *
 * <p>本次修复前 {@link DispatchTask} 实体未声明 {@code tenantId} 字段：
 * <ul>
 *   <li>MP {@code strictInsertFill} 对未声明字段 no-op，tenantId 不会被自动注入；</li>
 *   <li>{@code mes_dispatch_task.tenant_id DEFAULT 1} 兜底 → 任何租户创建派工
 *       都落到租户 1，形成跨租户污染。</li>
 * </ul>
 *
 * <p>本次修复后 {@link DispatchTask} 继承 {@link com.mes.common.core.BaseEntity}，
 * 而 {@link DispatchTaskServiceImpl#create} / {@link DispatchTaskServiceImpl#generateFromWorkOrder}
 * 显式调用 {@link TenantContextHolder#requireTenantId()} 绑定 tenantId；
 * 本测试类通过切换 TenantContext + ArgumentCaptor 证明：</p>
 * <ol>
 *   <li>租户 A 创建的派工 tenantId 必须 = A；</li>
 *   <li>租户 B 创建的派工 tenantId 必须 = B，绝不串到 A；</li>
 *   <li>generateFromWorkOrder 在多条派工生成时所有 tenantId 都统一为当前上下文；</li>
 *   <li>既无 TenantContext 时创建派工必须 fail-fast（IllegalStateException），拒绝写脏数据。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DispatchTaskTenantIsolationTest {

    @Mock private DispatchTaskMapper dispatchTaskMapper;
    @Mock private DispatchAssignmentMapper assignmentMapper;
    @Mock private WorkOrderMapper workOrderMapper;
    @Mock private WorkOrderTaskMapper workOrderTaskMapper;
    @Mock private IDispatchStatusLogService statusLogService;
    @Mock private IDispatchAssignmentService assignmentService;

    @InjectMocks private DispatchTaskServiceImpl dispatchTaskService;

    @BeforeEach
    void wireBaseMapper() {
        // MP 的 ServiceImpl 里 baseMapper 用字段注入；Mock 注入通常不会补到 super 的 private，
        // 用 ReflectionTestUtils 手工绑定，保证 save() / updateById() 下沉到 mock mapper。
        ReflectionTestUtils.setField(dispatchTaskService, "baseMapper", dispatchTaskMapper);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("create 租户 A：DispatchTask.tenantId 必须等于 TenantContext=A")
    void create_shouldBindTenantIdFromContext_tenantA() {
        // given: 租户 101，create DTO 完整
        TenantContextHolder.setTenantId(101L);
        DispatchTaskCreateDTO dto = sampleCreateDto();

        mockWorkOrderExists(dto.getWorkOrderId());
        when(dispatchTaskMapper.insert(any(DispatchTask.class))).thenReturn(1);

        // when
        dispatchTaskService.create(dto);

        // then: insert 的 entity.tenantId = 101
        ArgumentCaptor<DispatchTask> captor = ArgumentCaptor.forClass(DispatchTask.class);
        verify(dispatchTaskMapper).insert(captor.capture());
        DispatchTask inserted = captor.getValue();

        assertEquals(101L, inserted.getTenantId(),
                "租户 A 创建的派工必须携带 tenantId=101");
        assertEquals(dto.getOrderNo(), inserted.getOrderNo());
        assertEquals(DispatchStatus.UNASSIGNED.getCode(), inserted.getDispatchStatus());
    }

    @Test
    @DisplayName("create 租户 B：换租户后 tenantId 必须隔离为 B，不能串到 A")
    void create_shouldIsolateBetweenTenants_tenantB() {
        // given: 租户 202
        TenantContextHolder.setTenantId(202L);
        DispatchTaskCreateDTO dto = sampleCreateDto();

        mockWorkOrderExists(dto.getWorkOrderId());
        when(dispatchTaskMapper.insert(any(DispatchTask.class))).thenReturn(1);

        dispatchTaskService.create(dto);

        ArgumentCaptor<DispatchTask> captor = ArgumentCaptor.forClass(DispatchTask.class);
        verify(dispatchTaskMapper).insert(captor.capture());
        DispatchTask inserted = captor.getValue();

        assertEquals(202L, inserted.getTenantId(),
                "租户 B 创建的派工必须携带 tenantId=202，不能串到 A=101");
        assertNotEquals(101L, inserted.getTenantId(),
                "跨租户隔离保证：B 租户的派工 tenantId 绝不能出现 A=101");
    }

    @Test
    @DisplayName("generateFromWorkOrder：多条派工生成时所有 tenantId 都应来自当前上下文")
    void generateFromWorkOrder_shouldBindTenantIdForAllTasks() {
        // given: 租户 303，工单 10 号下有 3 条 workOrderTask
        TenantContextHolder.setTenantId(303L);

        WorkOrder wo = new WorkOrder();
        wo.setId(10L);
        wo.setWorkOrderNo("WO-TX-001");
        wo.setOrderNo("ORD-TX-001");

        WorkOrderTask t1 = workOrderTask(101L, 10L, "OP10", "锻造", 1);
        WorkOrderTask t2 = workOrderTask(102L, 10L, "OP20", "精加工", 2);
        WorkOrderTask t3 = workOrderTask(103L, 10L, "OP30", "热处理", 3);

        when(workOrderMapper.selectById(10L)).thenReturn(wo);
        when(workOrderTaskMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(t1, t2, t3));
        when(dispatchTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(dispatchTaskMapper.insert(any(DispatchTask.class))).thenReturn(1);

        // when
        dispatchTaskService.generateFromWorkOrder(10L);

        // then: 3 条 insert，每条 tenantId 都应是 303
        ArgumentCaptor<DispatchTask> captor = ArgumentCaptor.forClass(DispatchTask.class);
        verify(dispatchTaskMapper, times(3)).insert(captor.capture());
        List<DispatchTask> all = captor.getAllValues();

        assertEquals(3, all.size(), "应为工单的 3 条 workOrderTask 生成 3 条派工");
        for (DispatchTask d : all) {
            assertEquals(303L, d.getTenantId(),
                    "从工单生成的每条派工都必须携带 tenantId=303；"
                            + " 任意一条命中默认 1 都视为跨租户污染");
            assertEquals(DispatchStatus.UNASSIGNED.getCode(), d.getDispatchStatus());
        }
    }

    @Test
    @DisplayName("create 硬失败：无 TenantContext 时必须抛 IllegalStateException，禁止写库")
    void create_shouldFailFast_whenNoTenantContext() {
        // given: 不设置 TenantContext
        DispatchTaskCreateDTO dto = sampleCreateDto();

        // when + then
        assertThrows(IllegalStateException.class,
                () -> dispatchTaskService.create(dto),
                "无 TenantContext 时 create 必须抛 IllegalStateException，"
                        + " 避免 DB default 1 兜底导致的跨租户错挂");

        verify(dispatchTaskMapper, never()).insert(any(DispatchTask.class));
    }

    @Test
    @DisplayName("create 参数错误：workOrderId 为空时返回业务异常，禁止落库")
    void create_shouldRejectMissingWorkOrderId() {
        TenantContextHolder.setTenantId(101L);
        DispatchTaskCreateDTO dto = sampleCreateDto();
        dto.setWorkOrderId(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dispatchTaskService.create(dto));

        assertEquals("工单ID不能为空", ex.getMessage());
        verify(dispatchTaskMapper, never()).insert(any(DispatchTask.class));
    }

    @Test
    @DisplayName("generateFromWorkOrder 硬失败：无 TenantContext 时必须抛 IllegalStateException，禁止写库")
    void generateFromWorkOrder_shouldFailFast_whenNoTenantContext() {
        // given: 工单存在且有 1 条 workOrderTask，但不设 TenantContext
        WorkOrder wo = new WorkOrder();
        wo.setId(20L);
        wo.setWorkOrderNo("WO-NOCTX-001");
        wo.setOrderNo("ORD-NOCTX-001");
        WorkOrderTask t1 = workOrderTask(201L, 20L, "OP10", "X", 1);

        when(workOrderMapper.selectById(20L)).thenReturn(wo);
        when(workOrderTaskMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(t1));
        when(dispatchTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertThrows(IllegalStateException.class,
                () -> dispatchTaskService.generateFromWorkOrder(20L),
                "generateFromWorkOrder 必须在无 TenantContext 时 fail-fast");
        verify(dispatchTaskMapper, never()).insert(any(DispatchTask.class));
    }

    // ==================== helpers ====================

    private static DispatchTaskCreateDTO sampleCreateDto() {
        DispatchTaskCreateDTO dto = new DispatchTaskCreateDTO();
        dto.setWorkOrderId(10L);
        dto.setOrderNo("OP-T-001");
        dto.setProcessNo("WK-010");
        dto.setWorkName("数控精加工");
        dto.setPlanQty(new BigDecimal("5"));
        dto.setQtyUnit("PC");
        dto.setPlanStartTime(LocalDateTime.of(2026, 5, 1, 8, 0));
        dto.setPlanEndTime(LocalDateTime.of(2026, 5, 2, 17, 0));
        return dto;
    }

    private void mockWorkOrderExists(Long workOrderId) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(workOrderId);
        when(workOrderMapper.selectById(workOrderId)).thenReturn(workOrder);
    }

    private static WorkOrderTask workOrderTask(Long id, Long woId, String taskNo, String name, int seq) {
        WorkOrderTask t = new WorkOrderTask();
        t.setId(id);
        t.setWorkOrderId(woId);
        t.setTaskNo(taskNo);
        t.setTaskName(name);
        t.setSequenceNo(seq);
        return t;
    }
}
