package com.mes.dispatch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.common.exception.BusinessException;
import com.mes.dispatch.domain.dto.DispatchAssignDTO;
import com.mes.dispatch.domain.entity.DispatchAssignment;
import com.mes.dispatch.domain.entity.DispatchTask;
import com.mes.dispatch.enums.AssignType;
import com.mes.dispatch.enums.AssignmentStatus;
import com.mes.dispatch.enums.DispatchStatus;
import com.mes.dispatch.mapper.DispatchAssignmentMapper;
import com.mes.dispatch.mapper.DispatchTaskMapper;
import com.mes.dispatch.service.impl.DispatchAssignmentServiceImpl;
import com.mes.dispatch.service.impl.DispatchTaskServiceImpl;
import com.mes.workorder.domain.entity.WorkOrder;
import com.mes.workorder.domain.entity.WorkOrderTask;
import com.mes.workorder.mapper.WorkOrderMapper;
import com.mes.workorder.mapper.WorkOrderTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 派工任务与派工分配 Service 单元测试
 * <p>覆盖 {@link com.mes.dispatch.service.impl.DispatchTaskServiceImpl} 与
 * {@link com.mes.dispatch.service.impl.DispatchAssignmentServiceImpl}</p>
 */
@ExtendWith(MockitoExtension.class)
class DispatchServiceTest {

    @Mock
    private DispatchTaskMapper dispatchTaskMapper;
    @Mock
    private DispatchAssignmentMapper assignmentMapper;
    @Mock
    private WorkOrderMapper workOrderMapper;
    @Mock
    private WorkOrderTaskMapper workOrderTaskMapper;
    @Mock
    private IDispatchStatusLogService statusLogService;

    @InjectMocks
    private DispatchTaskServiceImpl dispatchTaskService;
    @InjectMocks
    private DispatchAssignmentServiceImpl dispatchAssignmentService;

    @BeforeEach
    void injectBaseMapper() {
        ReflectionTestUtils.setField(dispatchTaskService, "baseMapper", dispatchTaskMapper);
    }

    // —— DispatchTaskServiceImpl ——

    @Test
    @DisplayName("1. 从工单生成派工任务 - 正常")
    void generateFromWorkOrder_success() {
        WorkOrder wo = new WorkOrder();
        wo.setId(10L);
        wo.setWorkOrderNo("WO-001");
        wo.setOrderNo("ORD-001");

        WorkOrderTask t1 = workOrderTask(101L, 10L, "OP10", "工序一", 1);
        WorkOrderTask t2 = workOrderTask(102L, 10L, "OP20", "工序二", 2);

        when(workOrderMapper.selectById(10L)).thenReturn(wo);
        when(workOrderTaskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(t1, t2));
        when(dispatchTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(dispatchTaskMapper.insert(any(DispatchTask.class))).thenReturn(1);

        dispatchTaskService.generateFromWorkOrder(10L);

        verify(dispatchTaskMapper, times(2)).insert(argThat(dt ->
                DispatchStatus.UNASSIGNED.getCode().equals(dt.getDispatchStatus())
                        && "ORD-001".equals(dt.getOrderNo())));
    }

    @Test
    @DisplayName("2. 从工单生成派工任务 - 工单不存在抛异常")
    void generateFromWorkOrder_workOrderMissing() {
        when(workOrderMapper.selectById(99L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> dispatchTaskService.generateFromWorkOrder(99L));
        assertEquals("工单不存在", ex.getMessage());
    }

    @Test
    @DisplayName("3. 从工单生成派工任务 - 工单无工作清单抛异常")
    void generateFromWorkOrder_noWorkOrderTasks() {
        WorkOrder wo = new WorkOrder();
        wo.setId(10L);
        when(workOrderMapper.selectById(10L)).thenReturn(wo);
        when(workOrderTaskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dispatchTaskService.generateFromWorkOrder(10L));
        assertEquals("工单没有工作清单，无法生成派工任务", ex.getMessage());
    }

    @Test
    @DisplayName("4. 从工单生成派工任务 - 同一工单已存在派工任务抛异常")
    void generateFromWorkOrder_duplicate() {
        WorkOrder wo = new WorkOrder();
        wo.setId(10L);
        WorkOrderTask t1 = workOrderTask(101L, 10L, "OP10", "工序一", 1);
        when(workOrderMapper.selectById(10L)).thenReturn(wo);
        when(workOrderTaskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(t1));
        when(dispatchTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dispatchTaskService.generateFromWorkOrder(10L));
        assertEquals("该工单已生成派工任务，不可重复生成", ex.getMessage());
    }

    // —— DispatchAssignmentServiceImpl ——

    @Test
    @DisplayName("5. 人员派工 - 正常（UNASSIGNED→ASSIGNED）")
    void assignPerson_unassignedToAssigned() {
        DispatchTask task = dispatchTask(1L, DispatchStatus.UNASSIGNED);
        DispatchAssignDTO dto = assignDto(100L, "E001", "张三", new BigDecimal("10"));

        when(dispatchTaskMapper.selectById(1L)).thenReturn(task);
        when(assignmentMapper.insert(any(DispatchAssignment.class))).thenReturn(1);
        when(dispatchTaskMapper.updateById(any(DispatchTask.class))).thenReturn(1);

        dispatchAssignmentService.assignPerson(1L, dto);

        verify(assignmentMapper).insert(argThat(a ->
                AssignType.PERSON.getCode().equals(a.getAssignType())
                        && AssignmentStatus.ACTIVE.getCode().equals(a.getStatus())));
        verify(dispatchTaskMapper).updateById(argThat(t ->
                DispatchStatus.ASSIGNED.getCode().equals(t.getDispatchStatus())));
        verify(statusLogService).log(eq(1L), eq(DispatchStatus.UNASSIGNED.getCode()),
                eq(DispatchStatus.ASSIGNED.getCode()), any(), any());
    }

    @Test
    @DisplayName("6. 设备派工 - 正常")
    void assignDevice_success() {
        DispatchTask task = dispatchTask(2L, DispatchStatus.UNASSIGNED);
        DispatchAssignDTO dto = assignDto(200L, "CNC-01", "加工中心1", new BigDecimal("20"));

        when(dispatchTaskMapper.selectById(2L)).thenReturn(task);
        when(assignmentMapper.insert(any(DispatchAssignment.class))).thenReturn(1);
        when(dispatchTaskMapper.updateById(any(DispatchTask.class))).thenReturn(1);

        dispatchAssignmentService.assignDevice(2L, dto);

        verify(assignmentMapper).insert(argThat(a ->
                AssignType.DEVICE.getCode().equals(a.getAssignType())));
    }

    @Test
    @DisplayName("7. 班组派工 - 正常")
    void assignTeam_success() {
        DispatchTask task = dispatchTask(3L, DispatchStatus.REVOKED);
        DispatchAssignDTO dto = assignDto(300L, "TEAM-A", "甲班", new BigDecimal("30"));

        when(dispatchTaskMapper.selectById(3L)).thenReturn(task);
        when(assignmentMapper.insert(any(DispatchAssignment.class))).thenReturn(1);
        when(dispatchTaskMapper.updateById(any(DispatchTask.class))).thenReturn(1);

        dispatchAssignmentService.assignTeam(3L, dto);

        verify(assignmentMapper).insert(argThat(a ->
                AssignType.TEAM.getCode().equals(a.getAssignType())));
    }

    @Test
    @DisplayName("8. 非 UNASSIGNED/REVOKED 状态不允许派工")
    void assign_whenNotUnassignedOrRevoked_throws() {
        DispatchTask task = dispatchTask(4L, DispatchStatus.ASSIGNED);
        DispatchAssignDTO dto = assignDto(1L, "X", "Y", BigDecimal.ONE);
        when(dispatchTaskMapper.selectById(4L)).thenReturn(task);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dispatchAssignmentService.assignPerson(4L, dto));
        assertEquals("仅未分派或已撤销状态的任务可以派工", ex.getMessage());
    }

    @Test
    @DisplayName("9. 撤销分派 - 正常（ACTIVE→REVOKED）")
    void revoke_activeToRevoked() {
        DispatchAssignment assignment = assignment(50L, 500L, AssignmentStatus.ACTIVE);
        when(assignmentMapper.selectById(50L)).thenReturn(assignment);
        when(assignmentMapper.updateById(any(DispatchAssignment.class))).thenReturn(1);
        when(assignmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        dispatchAssignmentService.revoke(50L, "调整");

        verify(assignmentMapper).updateById(argThat(a ->
                AssignmentStatus.REVOKED.getCode().equals(a.getStatus())));
    }

    @Test
    @DisplayName("10. 撤销后无其他有效分配，任务状态回退 UNASSIGNED")
    void revoke_noOtherActive_taskBackToUnassigned() {
        DispatchAssignment assignment = assignment(51L, 501L, AssignmentStatus.ACTIVE);
        DispatchTask task = dispatchTask(501L, DispatchStatus.ASSIGNED);

        when(assignmentMapper.selectById(51L)).thenReturn(assignment);
        when(assignmentMapper.updateById(any(DispatchAssignment.class))).thenReturn(1);
        when(assignmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(dispatchTaskMapper.selectById(501L)).thenReturn(task);
        when(dispatchTaskMapper.updateById(any(DispatchTask.class))).thenReturn(1);

        dispatchAssignmentService.revoke(51L, "最后一条分配撤销");

        verify(dispatchTaskMapper).updateById(argThat(t ->
                DispatchStatus.UNASSIGNED.getCode().equals(t.getDispatchStatus())));
        verify(statusLogService).log(eq(501L), eq(DispatchStatus.ASSIGNED.getCode()),
                eq(DispatchStatus.UNASSIGNED.getCode()), eq("撤销分派"), any());
    }

    @Test
    @DisplayName("11. 撤销后仍有其他有效分配，任务状态保持 ASSIGNED")
    void revoke_stillHasActive_taskStaysAssigned() {
        DispatchAssignment assignment = assignment(52L, 502L, AssignmentStatus.ACTIVE);

        when(assignmentMapper.selectById(52L)).thenReturn(assignment);
        when(assignmentMapper.updateById(any(DispatchAssignment.class))).thenReturn(1);
        when(assignmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        dispatchAssignmentService.revoke(52L, "部分撤销");

        verify(dispatchTaskMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("12. 非 ACTIVE 状态不允许撤销")
    void revoke_whenNotActive_throws() {
        DispatchAssignment assignment = assignment(53L, 503L, AssignmentStatus.REVOKED);
        when(assignmentMapper.selectById(53L)).thenReturn(assignment);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dispatchAssignmentService.revoke(53L, "重复撤销"));
        assertEquals("仅有效状态的分配可以撤销", ex.getMessage());
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

    private static DispatchTask dispatchTask(Long id, DispatchStatus status) {
        DispatchTask t = new DispatchTask();
        t.setId(id);
        t.setWorkOrderId(1L);
        t.setDispatchStatus(status.getCode());
        return t;
    }

    private static DispatchAssignDTO assignDto(Long assigneeId, String code, String name, BigDecimal qty) {
        DispatchAssignDTO dto = new DispatchAssignDTO();
        dto.setAssigneeId(assigneeId);
        dto.setAssigneeCode(code);
        dto.setAssigneeName(name);
        dto.setAssignedQty(qty);
        return dto;
    }

    private static DispatchAssignment assignment(Long id, Long taskId, AssignmentStatus status) {
        DispatchAssignment a = new DispatchAssignment();
        a.setId(id);
        a.setDispatchTaskId(taskId);
        a.setStatus(status.getCode());
        return a;
    }
}
