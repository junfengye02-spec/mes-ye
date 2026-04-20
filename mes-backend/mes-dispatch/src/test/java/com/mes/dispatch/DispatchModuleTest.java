package com.mes.dispatch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.dispatch.domain.dto.DispatchAssignDTO;
import com.mes.dispatch.domain.entity.DispatchAssignment;
import com.mes.dispatch.domain.entity.DispatchTask;
import com.mes.dispatch.enums.AssignType;
import com.mes.dispatch.enums.AssignmentStatus;
import com.mes.dispatch.enums.DispatchStatus;
import com.mes.dispatch.mapper.DispatchAssignmentMapper;
import com.mes.dispatch.mapper.DispatchTaskMapper;
import com.mes.dispatch.service.IDispatchStatusLogService;
import com.mes.dispatch.service.impl.DispatchAssignmentServiceImpl;
import com.mes.dispatch.service.impl.DispatchTaskServiceImpl;
import com.mes.workorder.domain.entity.WorkOrder;
import com.mes.workorder.domain.entity.WorkOrderTask;
import com.mes.workorder.mapper.WorkOrderMapper;
import com.mes.workorder.mapper.WorkOrderTaskMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 派工管理模块单元测试
 * 覆盖派工任务生成、人员/设备/班组分配、撤销分配
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DispatchModuleTest {

    @Mock private DispatchTaskMapper dispatchTaskMapper;
    @Mock private DispatchAssignmentMapper assignmentMapper;
    @Mock private WorkOrderMapper workOrderMapper;
    @Mock private WorkOrderTaskMapper workOrderTaskMapper;
    @Mock private IDispatchStatusLogService statusLogService;

    @InjectMocks private DispatchTaskServiceImpl dispatchTaskService;
    @InjectMocks private DispatchAssignmentServiceImpl assignmentService;

    // ==================== 1. 派工任务生成测试 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 从工单生成派工任务 - 正常流程")
    void testGenerateFromWorkOrder_Success() {
        WorkOrder wo = new WorkOrder();
        wo.setId(1L);
        wo.setWorkOrderNo("WO-2024-001");
        wo.setOrderNo("ORD-2024-001");

        WorkOrderTask task1 = buildWoTask(101L, "OP-010", "粗车加工", 1);
        WorkOrderTask task2 = buildWoTask(102L, "OP-020", "精磨加工", 2);

        when(workOrderMapper.selectById(1L)).thenReturn(wo);
        when(workOrderTaskMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(task1, task2));
        when(dispatchTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(dispatchTaskMapper.insert(any(DispatchTask.class))).thenReturn(1);

        dispatchTaskService.generateFromWorkOrder(1L);

        verify(dispatchTaskMapper, times(2)).insert(argThat(dt ->
                DispatchStatus.UNASSIGNED.getCode().equals(dt.getDispatchStatus())));
    }

    @Test
    @Order(2)
    @DisplayName("1.2 重复生成派工任务 - 应拒绝")
    void testGenerateFromWorkOrder_Duplicate() {
        WorkOrder wo = new WorkOrder();
        wo.setId(1L);

        WorkOrderTask task = buildWoTask(101L, "OP-010", "粗车", 1);

        when(workOrderMapper.selectById(1L)).thenReturn(wo);
        when(workOrderTaskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(dispatchTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(Exception.class, () -> dispatchTaskService.generateFromWorkOrder(1L),
                "已存在派工任务时不应允许重复生成");
    }

    @Test
    @Order(3)
    @DisplayName("1.3 工单不存在 - 应拒绝")
    void testGenerateFromWorkOrder_WorkOrderNotExist() {
        when(workOrderMapper.selectById(999L)).thenReturn(null);

        assertThrows(Exception.class, () -> dispatchTaskService.generateFromWorkOrder(999L));
    }

    @Test
    @Order(4)
    @DisplayName("1.4 工单无工作清单 - 应拒绝")
    void testGenerateFromWorkOrder_NoTasks() {
        WorkOrder wo = new WorkOrder();
        wo.setId(1L);

        when(workOrderMapper.selectById(1L)).thenReturn(wo);
        when(workOrderTaskMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        assertThrows(Exception.class, () -> dispatchTaskService.generateFromWorkOrder(1L));
    }

    // ==================== 2. 人员分配测试 ====================

    @Test
    @Order(10)
    @DisplayName("2.1 人员分配 - 未分派任务应成功")
    void testAssignPerson_Unassigned() {
        DispatchTask task = buildDispatchTask(1L, DispatchStatus.UNASSIGNED);
        DispatchAssignDTO dto = buildAssignDTO("EMP-001", "张三", new BigDecimal("50"));

        when(dispatchTaskMapper.selectById(1L)).thenReturn(task);
        when(assignmentMapper.insert(any(DispatchAssignment.class))).thenReturn(1);
        when(dispatchTaskMapper.updateById(any(DispatchTask.class))).thenReturn(1);

        assignmentService.assignPerson(1L, dto);

        verify(assignmentMapper).insert(argThat(a -> {
            assertEquals(AssignType.PERSON.getCode(), a.getAssignType());
            assertEquals("EMP-001", a.getAssigneeCode());
            assertEquals(AssignmentStatus.ACTIVE.getCode(), a.getStatus());
            return true;
        }));
        verify(dispatchTaskMapper).updateById(argThat(t ->
                DispatchStatus.ASSIGNED.getCode().equals(t.getDispatchStatus())));
    }

    // ==================== 3. 设备分配测试 ====================

    @Test
    @Order(20)
    @DisplayName("3.1 设备分配 - 未分派任务应成功")
    void testAssignDevice_Unassigned() {
        DispatchTask task = buildDispatchTask(2L, DispatchStatus.UNASSIGNED);
        DispatchAssignDTO dto = buildAssignDTO("CNC-001", "CNC加工中心1号", new BigDecimal("100"));

        when(dispatchTaskMapper.selectById(2L)).thenReturn(task);
        when(assignmentMapper.insert(any(DispatchAssignment.class))).thenReturn(1);
        when(dispatchTaskMapper.updateById(any(DispatchTask.class))).thenReturn(1);

        assignmentService.assignDevice(2L, dto);

        verify(assignmentMapper).insert(argThat(a ->
                AssignType.DEVICE.getCode().equals(a.getAssignType())));
    }

    // ==================== 4. 班组分配测试 ====================

    @Test
    @Order(30)
    @DisplayName("4.1 班组分配 - 已撤销的任务应可再次派工")
    void testAssignTeam_RevokedTask() {
        DispatchTask task = buildDispatchTask(3L, DispatchStatus.REVOKED);
        DispatchAssignDTO dto = buildAssignDTO("TEAM-A01", "甲班一组", new BigDecimal("80"));

        when(dispatchTaskMapper.selectById(3L)).thenReturn(task);
        when(assignmentMapper.insert(any(DispatchAssignment.class))).thenReturn(1);
        when(dispatchTaskMapper.updateById(any(DispatchTask.class))).thenReturn(1);

        assignmentService.assignTeam(3L, dto);

        verify(assignmentMapper).insert(argThat(a ->
                AssignType.TEAM.getCode().equals(a.getAssignType())));
    }

    // ==================== 5. 已分派任务不可再派工 ====================

    @Test
    @Order(40)
    @DisplayName("5.1 已分派任务再次派工 - 应拒绝")
    void testAssign_AlreadyAssigned() {
        DispatchTask task = buildDispatchTask(4L, DispatchStatus.ASSIGNED);
        DispatchAssignDTO dto = buildAssignDTO("EMP-002", "李四", new BigDecimal("30"));

        when(dispatchTaskMapper.selectById(4L)).thenReturn(task);

        assertThrows(Exception.class, () -> assignmentService.assignPerson(4L, dto),
                "已分派的任务不应允许再次派工");
    }

    // ==================== 6. 撤销分配测试 ====================

    @Test
    @Order(50)
    @DisplayName("6.1 撤销分配 - 无其他有效分配时任务回退 UNASSIGNED")
    void testRevoke_NoOtherActive() {
        DispatchAssignment assignment = buildAssignment(1L, 100L, AssignmentStatus.ACTIVE);
        DispatchTask task = buildDispatchTask(100L, DispatchStatus.ASSIGNED);

        when(assignmentMapper.selectById(1L)).thenReturn(assignment);
        when(assignmentMapper.updateById(any(DispatchAssignment.class))).thenReturn(1);
        when(assignmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(dispatchTaskMapper.selectById(100L)).thenReturn(task);
        when(dispatchTaskMapper.updateById(any(DispatchTask.class))).thenReturn(1);

        assignmentService.revoke(1L, "人员调整");

        verify(assignmentMapper).updateById(argThat(a ->
                AssignmentStatus.REVOKED.getCode().equals(a.getStatus())));
        verify(dispatchTaskMapper).updateById(argThat(t ->
                DispatchStatus.UNASSIGNED.getCode().equals(t.getDispatchStatus())));
    }

    @Test
    @Order(51)
    @DisplayName("6.2 撤销分配 - 有其他有效分配时任务保持 ASSIGNED")
    void testRevoke_HasOtherActive() {
        DispatchAssignment assignment = buildAssignment(1L, 100L, AssignmentStatus.ACTIVE);

        when(assignmentMapper.selectById(1L)).thenReturn(assignment);
        when(assignmentMapper.updateById(any(DispatchAssignment.class))).thenReturn(1);
        when(assignmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assignmentService.revoke(1L, "部分撤销");

        verify(assignmentMapper).updateById(argThat(a ->
                AssignmentStatus.REVOKED.getCode().equals(a.getStatus())));
        verify(dispatchTaskMapper, never()).updateById(any());
    }

    @Test
    @Order(52)
    @DisplayName("6.3 撤销非 ACTIVE 的分配 - 应拒绝")
    void testRevoke_NotActive() {
        DispatchAssignment assignment = buildAssignment(1L, 100L, AssignmentStatus.REVOKED);

        when(assignmentMapper.selectById(1L)).thenReturn(assignment);

        assertThrows(Exception.class, () -> assignmentService.revoke(1L, "test"),
                "已撤销的分配不应允许再次撤销");
    }

    // ==================== 7. 枚举完整性测试 ====================

    @Test
    @Order(60)
    @DisplayName("7.1 DispatchStatus 枚举完整性")
    void testDispatchStatusEnum() {
        assertNotNull(DispatchStatus.UNASSIGNED);
        assertNotNull(DispatchStatus.ASSIGNED);
        assertNotNull(DispatchStatus.REVOKED);
    }

    @Test
    @Order(61)
    @DisplayName("7.2 AssignType 枚举完整性")
    void testAssignTypeEnum() {
        assertNotNull(AssignType.PERSON);
        assertNotNull(AssignType.DEVICE);
        assertNotNull(AssignType.TEAM);
    }

    // ==================== 辅助方法 ====================

    private WorkOrderTask buildWoTask(Long id, String taskNo, String taskName, int seq) {
        WorkOrderTask t = new WorkOrderTask();
        t.setId(id);
        t.setTaskNo(taskNo);
        t.setTaskName(taskName);
        t.setSequenceNo(seq);
        return t;
    }

    private DispatchTask buildDispatchTask(Long id, DispatchStatus status) {
        DispatchTask task = new DispatchTask();
        task.setId(id);
        task.setDispatchStatus(status.getCode());
        task.setWorkOrderId(1L);
        return task;
    }

    private DispatchAssignDTO buildAssignDTO(String code, String name, BigDecimal qty) {
        DispatchAssignDTO dto = new DispatchAssignDTO();
        dto.setAssigneeCode(code);
        dto.setAssigneeName(name);
        dto.setAssignedQty(qty);
        return dto;
    }

    private DispatchAssignment buildAssignment(Long id, Long taskId, AssignmentStatus status) {
        DispatchAssignment a = new DispatchAssignment();
        a.setId(id);
        a.setDispatchTaskId(taskId);
        a.setStatus(status.getCode());
        return a;
    }
}
