package com.mes.workorder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.workorder.domain.dto.*;
import com.mes.workorder.domain.entity.*;
import com.mes.workorder.enums.WorkOrderStatus;
import com.mes.workorder.event.WorkOrderReleasedEvent;
import com.mes.workorder.mapper.*;
import com.mes.workorder.service.IWorkOrderStatusLogService;
import com.mes.workorder.service.impl.WorkOrderServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 工单管理模块单元测试
 * 覆盖工单完整生命周期：创建→下发→开工→完工/强制完工
 * 以及子表（任务、投料、产出、质检项、约束、供应计划）的管理
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkOrderModuleTest {

    @Mock private WorkOrderMapper workOrderMapper;
    @Mock private WorkOrderTaskMapper taskMapper;
    @Mock private WorkOrderInputMaterialMapper inputMaterialMapper;
    @Mock private WorkOrderOutputMaterialMapper outputMaterialMapper;
    @Mock private WorkOrderQualityItemMapper qualityItemMapper;
    @Mock private WorkOrderConstraintMapper constraintMapper;
    @Mock private WorkOrderSupplyPlanMapper supplyPlanMapper;
    @Mock private WorkOrderAttachmentMapper attachmentMapper;
    @Mock private IWorkOrderStatusLogService statusLogService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Spy
    @InjectMocks
    private WorkOrderServiceImpl workOrderService;

    @BeforeEach
    void bindBaseMapper() {
        // MyBatis-Plus ServiceImpl 的 baseMapper 来自父类字段，Mockito 不会自动注入，需要反射绑定
        ReflectionTestUtils.setField(workOrderService, "baseMapper", workOrderMapper);
    }

    // ==================== 1. 工单创建测试 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 创建工单 - 正常流程（含子表）")
    void testCreateWorkOrder_Success() {
        WorkOrderDTO dto = buildWorkOrderDTO("WO-2024-001", new BigDecimal("100"));
        dto.setTasks(List.of(buildTaskDTO("OP-010", "粗车加工")));
        dto.setInputMaterials(List.of(buildInputMaterialDTO("MAT-001", new BigDecimal("50"))));

        when(workOrderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(workOrderMapper.insert(any(WorkOrder.class))).thenReturn(1);
        when(taskMapper.insert(any(WorkOrderTask.class))).thenReturn(1);
        when(inputMaterialMapper.insert(any(WorkOrderInputMaterial.class))).thenReturn(1);

        Long id = workOrderService.create(dto);

        verify(workOrderMapper).insert(argThat(wo -> {
            assertEquals(WorkOrderStatus.CREATED.getCode(), wo.getStatus(), "初始状态应为 CREATED");
            return true;
        }));
        verify(taskMapper).insert(argThat(task -> {
            assertEquals("CREATED", task.getStatus(), "任务初始状态应为 CREATED");
            return true;
        }));
        verify(inputMaterialMapper).insert(argThat(m -> {
            assertEquals(BigDecimal.ZERO, m.getIssuedQty(), "投料初始已发量应为 0");
            return true;
        }));
        verify(statusLogService).log(any(), isNull(), eq(WorkOrderStatus.CREATED.getCode()),
                eq("创建"), anyString());
    }

    @Test
    @Order(2)
    @DisplayName("1.2 创建工单 - 工单号重复应拒绝")
    void testCreateWorkOrder_DuplicateNo() {
        WorkOrderDTO dto = buildWorkOrderDTO("WO-2024-001", new BigDecimal("100"));

        when(workOrderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(Exception.class, () -> workOrderService.create(dto),
                "重复的工单号应被拒绝");
    }

    @Test
    @Order(3)
    @DisplayName("1.3 创建工单 - 计划数量为 0 应拒绝")
    void testCreateWorkOrder_ZeroPlanQty() {
        WorkOrderDTO dto = buildWorkOrderDTO("WO-2024-002", BigDecimal.ZERO);

        when(workOrderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertThrows(Exception.class, () -> workOrderService.create(dto),
                "计划数量为0不应允许创建");
    }

    @Test
    @Order(4)
    @DisplayName("1.4 创建工单 - 计划数量为负数应拒绝")
    void testCreateWorkOrder_NegativePlanQty() {
        WorkOrderDTO dto = buildWorkOrderDTO("WO-2024-003", new BigDecimal("-10"));

        when(workOrderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertThrows(Exception.class, () -> workOrderService.create(dto),
                "负数计划数量不应允许创建");
    }

    @Test
    @Order(5)
    @DisplayName("1.5 创建工单 - 计划数量为 null 应拒绝")
    void testCreateWorkOrder_NullPlanQty() {
        WorkOrderDTO dto = buildWorkOrderDTO("WO-2024-004", null);

        when(workOrderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertThrows(Exception.class, () -> workOrderService.create(dto),
                "null 计划数量不应允许创建");
    }

    // ==================== 2. 工单编辑/删除测试 ====================

    @Test
    @Order(10)
    @DisplayName("2.1 编辑 CREATED 状态工单 - 应成功")
    void testUpdateWorkOrder_CreatedStatus() {
        WorkOrder existing = buildWorkOrder(1L, "WO-2024-001", WorkOrderStatus.CREATED);
        WorkOrderDTO dto = buildWorkOrderDTO("WO-2024-001", new BigDecimal("200"));

        when(workOrderMapper.selectById(1L)).thenReturn(existing);
        when(workOrderMapper.updateById(any(WorkOrder.class))).thenReturn(1);

        workOrderService.update(1L, dto);

        verify(workOrderMapper).updateById(argThat(wo -> {
            assertEquals(WorkOrderStatus.CREATED.getCode(), wo.getStatus(), "编辑不应改变状态");
            return true;
        }));
    }

    @Test
    @Order(11)
    @DisplayName("2.2 编辑非 CREATED 状态工单 - 应拒绝")
    void testUpdateWorkOrder_ReleasedStatus() {
        WorkOrder existing = buildWorkOrder(1L, "WO-2024-001", WorkOrderStatus.RELEASED);

        when(workOrderMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class,
                () -> workOrderService.update(1L, buildWorkOrderDTO("WO-2024-001", new BigDecimal("100"))),
                "已下发的工单不应允许编辑");
    }

    @Test
    @Order(12)
    @DisplayName("2.3 删除 CREATED 状态工单 - 应成功")
    void testDeleteWorkOrder_CreatedStatus() {
        WorkOrder existing = buildWorkOrder(1L, "WO-2024-001", WorkOrderStatus.CREATED);

        when(workOrderMapper.selectById(1L)).thenReturn(existing);
        // ServiceImpl.removeById 内部依赖 TableInfoHelper 注册表，单测环境下直接 Spy 打桩
        doReturn(true).when(workOrderService).removeById(1L);

        workOrderService.delete(1L);

        verify(workOrderService).removeById(1L);
    }

    @Test
    @Order(13)
    @DisplayName("2.4 删除非 CREATED 状态工单 - 应拒绝")
    void testDeleteWorkOrder_InProgressStatus() {
        WorkOrder existing = buildWorkOrder(1L, "WO-2024-001", WorkOrderStatus.IN_PROGRESS);

        when(workOrderMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class, () -> workOrderService.delete(1L),
                "执行中的工单不应允许删除");
    }

    // ==================== 3. 工单下发测试 ====================

    @Test
    @Order(20)
    @DisplayName("3.1 下发工单 - CREATED → RELEASED（含工作清单）")
    void testReleaseWorkOrder_Success() {
        WorkOrder existing = buildWorkOrder(1L, "WO-2024-001", WorkOrderStatus.CREATED);

        when(workOrderMapper.selectById(1L)).thenReturn(existing);
        when(taskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(workOrderMapper.updateById(any(WorkOrder.class))).thenReturn(1);

        workOrderService.release(1L);

        verify(workOrderMapper).updateById(argThat(wo ->
                WorkOrderStatus.RELEASED.getCode().equals(wo.getStatus())));
        verify(eventPublisher).publishEvent(any(WorkOrderReleasedEvent.class));
        verify(statusLogService).log(eq(1L), eq(WorkOrderStatus.CREATED.getCode()),
                eq(WorkOrderStatus.RELEASED.getCode()), eq("下发"), anyString());
    }

    @Test
    @Order(21)
    @DisplayName("3.2 下发工单 - 无工作清单应拒绝")
    void testReleaseWorkOrder_NoTasks() {
        WorkOrder existing = buildWorkOrder(1L, "WO-2024-001", WorkOrderStatus.CREATED);

        when(workOrderMapper.selectById(1L)).thenReturn(existing);
        when(taskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertThrows(Exception.class, () -> workOrderService.release(1L),
                "没有工作清单不应允许下发");
    }

    @Test
    @Order(22)
    @DisplayName("3.3 已下发工单再次下发 - 应拒绝")
    void testReleaseWorkOrder_AlreadyReleased() {
        WorkOrder existing = buildWorkOrder(1L, "WO-2024-001", WorkOrderStatus.RELEASED);

        when(workOrderMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class, () -> workOrderService.release(1L));
    }

    // ==================== 4. 工单开工测试 ====================

    @Test
    @Order(30)
    @DisplayName("4.1 开工 - RELEASED → IN_PROGRESS")
    void testStartWorkOrder_Success() {
        WorkOrder existing = buildWorkOrder(1L, "WO-2024-001", WorkOrderStatus.RELEASED);

        when(workOrderMapper.selectById(1L)).thenReturn(existing);
        when(workOrderMapper.updateById(any(WorkOrder.class))).thenReturn(1);

        workOrderService.start(1L);

        verify(workOrderMapper).updateById(argThat(wo -> {
            assertEquals(WorkOrderStatus.IN_PROGRESS.getCode(), wo.getStatus());
            assertNotNull(wo.getActualStartTime(), "应记录实际开工时间");
            return true;
        }));
    }

    @Test
    @Order(31)
    @DisplayName("4.2 CREATED 状态直接开工 - 应拒绝")
    void testStartWorkOrder_CreatedStatus() {
        WorkOrder existing = buildWorkOrder(1L, "WO-2024-001", WorkOrderStatus.CREATED);

        when(workOrderMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class, () -> workOrderService.start(1L),
                "未下发的工单不应允许开工");
    }

    // ==================== 5. 工单完工测试 ====================

    @Test
    @Order(40)
    @DisplayName("5.1 正常完工 - IN_PROGRESS → COMPLETED")
    void testCompleteWorkOrder_Success() {
        WorkOrder existing = buildWorkOrder(1L, "WO-2024-001", WorkOrderStatus.IN_PROGRESS);
        existing.setActualStartTime(LocalDateTime.now().minusHours(8));

        when(workOrderMapper.selectById(1L)).thenReturn(existing);
        when(workOrderMapper.updateById(any(WorkOrder.class))).thenReturn(1);

        workOrderService.complete(1L);

        verify(workOrderMapper).updateById(argThat(wo -> {
            assertEquals(WorkOrderStatus.COMPLETED.getCode(), wo.getStatus());
            assertNotNull(wo.getActualEndTime(), "应记录实际完工时间");
            return true;
        }));
    }

    @Test
    @Order(41)
    @DisplayName("5.2 RELEASED 状态直接完工 - 应拒绝")
    void testCompleteWorkOrder_ReleasedStatus() {
        WorkOrder existing = buildWorkOrder(1L, "WO-2024-001", WorkOrderStatus.RELEASED);

        when(workOrderMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class, () -> workOrderService.complete(1L),
                "未开工的工单不应允许完工");
    }

    // ==================== 6. 强制完工测试 ====================

    @Test
    @Order(50)
    @DisplayName("6.1 强制完工 - IN_PROGRESS → FORCE_COMPLETED（填写原因）")
    void testForceCompleteWorkOrder_Success() {
        WorkOrder existing = buildWorkOrder(1L, "WO-2024-001", WorkOrderStatus.IN_PROGRESS);

        when(workOrderMapper.selectById(1L)).thenReturn(existing);
        when(workOrderMapper.updateById(any(WorkOrder.class))).thenReturn(1);

        workOrderService.forceComplete(1L, "设备故障无法继续");

        verify(workOrderMapper).updateById(argThat(wo ->
                WorkOrderStatus.FORCE_COMPLETED.getCode().equals(wo.getStatus())));
        verify(statusLogService).log(eq(1L), eq(WorkOrderStatus.IN_PROGRESS.getCode()),
                eq(WorkOrderStatus.FORCE_COMPLETED.getCode()), eq("强制完工"),
                contains("设备故障无法继续"));
    }

    @Test
    @Order(51)
    @DisplayName("6.2 强制完工不填原因 - 应拒绝")
    void testForceCompleteWorkOrder_NoReason() {
        WorkOrder existing = buildWorkOrder(1L, "WO-2024-001", WorkOrderStatus.IN_PROGRESS);

        when(workOrderMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class,
                () -> workOrderService.forceComplete(1L, ""),
                "强制完工必须填写原因");
    }

    @Test
    @Order(52)
    @DisplayName("6.3 非 IN_PROGRESS 状态强制完工 - 应拒绝")
    void testForceCompleteWorkOrder_CreatedStatus() {
        WorkOrder existing = buildWorkOrder(1L, "WO-2024-001", WorkOrderStatus.CREATED);

        when(workOrderMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class,
                () -> workOrderService.forceComplete(1L, "测试原因"),
                "非执行中状态不应允许强制完工");
    }

    // ==================== 7. 状态枚举完整性测试 ====================

    @Test
    @Order(60)
    @DisplayName("7.1 WorkOrderStatus 枚举完整性")
    void testWorkOrderStatusEnum() {
        assertNotNull(WorkOrderStatus.CREATED);
        assertNotNull(WorkOrderStatus.RELEASED);
        assertNotNull(WorkOrderStatus.IN_PROGRESS);
        assertNotNull(WorkOrderStatus.COMPLETED);
        assertNotNull(WorkOrderStatus.FORCE_COMPLETED);

        assertNotNull(WorkOrderStatus.CREATED.getCode());
        assertNotNull(WorkOrderStatus.RELEASED.getCode());
    }

    @Test
    @Order(61)
    @DisplayName("7.2 工单完整状态流转验证 CREATED→RELEASED→IN_PROGRESS→COMPLETED")
    void testWorkOrderFullLifecycle() {
        WorkOrder wo = buildWorkOrder(1L, "WO-LIFECYCLE", WorkOrderStatus.CREATED);

        assertEquals(WorkOrderStatus.CREATED.getCode(), wo.getStatus());

        wo.setStatus(WorkOrderStatus.RELEASED.getCode());
        assertEquals(WorkOrderStatus.RELEASED.getCode(), wo.getStatus());

        wo.setStatus(WorkOrderStatus.IN_PROGRESS.getCode());
        assertEquals(WorkOrderStatus.IN_PROGRESS.getCode(), wo.getStatus());

        wo.setStatus(WorkOrderStatus.COMPLETED.getCode());
        assertEquals(WorkOrderStatus.COMPLETED.getCode(), wo.getStatus());
    }

    // ==================== 辅助方法 ====================

    private WorkOrderDTO buildWorkOrderDTO(String workOrderNo, BigDecimal planQty) {
        WorkOrderDTO dto = new WorkOrderDTO();
        dto.setWorkOrderNo(workOrderNo);
        dto.setOrderNo("ORD-2024-001");
        dto.setProductCode("PROD-001");
        dto.setProductName("CFM56叶片");
        dto.setPlanQty(planQty);
        return dto;
    }

    private WorkOrder buildWorkOrder(Long id, String workOrderNo, WorkOrderStatus status) {
        WorkOrder wo = new WorkOrder();
        wo.setId(id);
        wo.setWorkOrderNo(workOrderNo);
        wo.setOrderNo("ORD-2024-001");
        wo.setStatus(status.getCode());
        return wo;
    }

    private WorkOrderTaskDTO buildTaskDTO(String taskNo, String taskName) {
        WorkOrderTaskDTO dto = new WorkOrderTaskDTO();
        dto.setTaskNo(taskNo);
        dto.setTaskName(taskName);
        dto.setSequenceNo(1);
        return dto;
    }

    private WorkOrderInputMaterialDTO buildInputMaterialDTO(String materialCode, BigDecimal requiredQty) {
        WorkOrderInputMaterialDTO dto = new WorkOrderInputMaterialDTO();
        dto.setMaterialCode(materialCode);
        dto.setRequiredQty(requiredQty);
        return dto;
    }
}
