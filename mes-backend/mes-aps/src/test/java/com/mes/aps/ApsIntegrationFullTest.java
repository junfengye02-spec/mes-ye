package com.mes.aps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mes.aps.client.ApsClient;
import com.mes.aps.domain.entity.ApsSyncLog;
import com.mes.aps.domain.entity.ApsSyncQueue;
import com.mes.aps.domain.vo.*;
import com.mes.aps.enums.SyncDirection;
import com.mes.aps.enums.SyncStatus;
import com.mes.aps.enums.SyncType;
import com.mes.aps.mapper.ApsSyncQueueMapper;
import com.mes.aps.service.*;
import com.mes.aps.service.impl.*;
import com.mes.basic.domain.entity.Material;
import com.mes.basic.domain.entity.WorkCenter;
import com.mes.basic.mapper.MaterialMapper;
import com.mes.basic.mapper.WorkCenterMapper;
import com.mes.dispatch.domain.entity.DispatchAssignment;
import com.mes.dispatch.domain.entity.DispatchTask;
import com.mes.dispatch.mapper.DispatchAssignmentMapper;
import com.mes.dispatch.mapper.DispatchTaskMapper;
import com.mes.material.domain.entity.MaterialRequisition;
import com.mes.material.mapper.MaterialRequisitionMapper;
import com.mes.plan.mapper.OrderPlanMapper;
import com.mes.process.domain.entity.ManufacturingBom;
import com.mes.process.domain.entity.ManufacturingBomItem;
import com.mes.process.domain.entity.ProcessInfo;
import com.mes.process.mapper.ManufacturingBomItemMapper;
import com.mes.process.mapper.ManufacturingBomMapper;
import com.mes.process.mapper.ProcessInfoMapper;
import com.mes.quality.domain.entity.ShiftHandover;
import com.mes.quality.mapper.ShiftHandoverMapper;
import com.mes.team.domain.entity.ProductionTeam;
import com.mes.team.mapper.ProductionTeamMapper;
import com.mes.workorder.domain.entity.*;
import com.mes.workorder.mapper.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * APS 扩展集成全流程测试
 * <p>覆盖主数据同步、执行反馈、APS下发回调全部场景</p>
 */
@ExtendWith(MockitoExtension.class)
// 幂等服务默认返回 false，需在各用例显式放通；整体放宽 Strictness 避免 UnnecessaryStubbing
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApsIntegrationFullTest {

    private ObjectMapper objectMapper;

    // ===== 主数据同步依赖 =====
    @Mock private ApsClient apsClient;
    @Mock private IApsSyncConfigService configService;
    @Mock private IApsSyncLogService syncLogService;
    @Mock private WorkCenterMapper workCenterMapper;
    @Mock private ProcessInfoMapper processInfoMapper;
    @Mock private ManufacturingBomMapper bomMapper;
    @Mock private ManufacturingBomItemMapper bomItemMapper;
    @Mock private MaterialMapper materialMapper;
    @Mock private ProductionTeamMapper teamMapper;

    // ===== 执行反馈依赖 =====
    @Mock private IApsUpstreamSyncService upstreamSyncService;
    @Mock private DispatchTaskMapper dispatchTaskMapper;
    @Mock private DispatchAssignmentMapper dispatchAssignmentMapper;
    @Mock private ShiftHandoverMapper shiftHandoverMapper;
    @Mock private WorkOrderMapper workOrderMapper;
    @Mock private WorkOrderInputMaterialMapper inputMaterialMapper;
    @Mock private WorkOrderConstraintMapper constraintMapper;
    @Mock private WorkOrderSupplyPlanMapper supplyPlanMapper;
    @Mock private MaterialRequisitionMapper requisitionMapper;
    @Mock private ApsSyncQueueMapper syncQueueMapper;
    @Mock private OrderPlanMapper orderPlanMapper;
    // P2 升级后 ApsExtendedCallbackServiceImpl 新增了幂等服务依赖
    @Mock private com.mes.aps.service.ApsCallbackIdempotencyService idempotencyService;

    // ===== 被测服务 =====
    private ApsMasterDataSyncServiceImpl masterDataSyncService;
    private ApsExecutionFeedbackServiceImpl feedbackService;
    private ApsExtendedCallbackServiceImpl callbackService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        masterDataSyncService = new ApsMasterDataSyncServiceImpl(
                apsClient, configService, syncLogService, objectMapper,
                workCenterMapper, processInfoMapper, bomMapper, bomItemMapper,
                materialMapper, teamMapper);

        feedbackService = new ApsExecutionFeedbackServiceImpl(
                upstreamSyncService, objectMapper,
                dispatchTaskMapper, dispatchAssignmentMapper, shiftHandoverMapper,
                workOrderMapper, inputMaterialMapper, constraintMapper,
                supplyPlanMapper, requisitionMapper);

        callbackService = new ApsExtendedCallbackServiceImpl(
                syncLogService, objectMapper,
                workOrderMapper, dispatchTaskMapper, dispatchAssignmentMapper,
                idempotencyService);

        // 默认放通幂等校验：测试聚焦业务分支，由各用例按需再 override
        when(idempotencyService.tryAcquire(anyString(), anyString())).thenReturn(true);
    }

    // ==================== 1. 主数据同步测试 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 工作中心主数据同步 - 正常流程")
    void testSyncWorkCenters_Success() {
        // 准备测试数据
        WorkCenter wc1 = new WorkCenter();
        wc1.setWorkCenterCode("WC-001");
        wc1.setWorkCenterName("CNC加工中心1号");
        wc1.setWorkCenterCategory("MACHINING");
        wc1.setEfficiency(new BigDecimal("0.95"));
        wc1.setResourceCapacity(new BigDecimal("480"));
        wc1.setBatchQty(new BigDecimal("100"));
        wc1.setProcessNoInterrupt(1);
        wc1.setProcessNoCrossDay(0);

        WorkCenter wc2 = new WorkCenter();
        wc2.setWorkCenterCode("WC-002");
        wc2.setWorkCenterName("磨床2号");
        wc2.setEfficiency(new BigDecimal("0.88"));
        wc2.setResourceCapacity(new BigDecimal("420"));

        when(workCenterMapper.selectList(any())).thenReturn(List.of(wc1, wc2));
        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());
        when(apsClient.post(anyString(), any(), eq(Map.class))).thenReturn(Map.of("status", "ok"));

        // 执行
        ApsSyncResultVO result = masterDataSyncService.syncWorkCenters();

        // 验证
        assertNotNull(result);
        assertEquals(SyncStatus.SUCCESS.getCode(), result.getStatus());
        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailCount());

        verify(apsClient).post(eq("/api/mes/master-data/work-centers"), any(), eq(Map.class));
        verify(syncLogService).completeLog(any(), eq(2), eq(2), eq(0), isNull());

        System.out.println("✓ 1.1 工作中心同步成功: 2条数据");
    }

    @Test
    @Order(2)
    @DisplayName("1.2 工艺路线同步 - 正常流程")
    void testSyncProcessRoutes_Success() {
        ProcessInfo p1 = new ProcessInfo();
        p1.setProcessNo("OP-010");
        p1.setProcessName("粗车加工");
        p1.setProcessCode("PC-001");
        p1.setHandleTime(new BigDecimal("120"));
        p1.setDisassembleTime(new BigDecimal("15"));
        p1.setInstallTime(new BigDecimal("20"));
        p1.setWorkCenterId(1L);

        when(processInfoMapper.selectList(any())).thenReturn(List.of(p1));
        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());
        when(apsClient.post(anyString(), any(), eq(Map.class))).thenReturn(Map.of("status", "ok"));

        ApsSyncResultVO result = masterDataSyncService.syncProcessRoutes();

        assertNotNull(result);
        assertEquals(SyncStatus.SUCCESS.getCode(), result.getStatus());
        assertEquals(1, result.getTotalCount());
        verify(apsClient).post(eq("/api/mes/master-data/process-routes"), any(), eq(Map.class));

        System.out.println("✓ 1.2 工艺路线同步成功: 1条数据");
    }

    @Test
    @Order(3)
    @DisplayName("1.3 制造BOM同步 - 含BOM明细")
    void testSyncBoms_WithItems() {
        ManufacturingBom bom = new ManufacturingBom();
        bom.setId(101L);
        bom.setBomCode("BOM-2024-001");
        bom.setBomName("CFM56叶片BOM");
        bom.setProductCode("PROD-001");
        bom.setBomVersion("V2.0");
        bom.setStatus("ACTIVE");
        bom.setEffectiveDate(LocalDate.of(2024, 1, 1));

        ManufacturingBomItem item = new ManufacturingBomItem();
        item.setMaterialCode("MAT-001");
        item.setMaterialName("钛合金棒料");
        item.setQuantity(new BigDecimal("1.00"));
        item.setUnit("PCS");
        item.setIsKeyPart(1);

        when(bomMapper.selectList(any())).thenReturn(List.of(bom));
        when(bomItemMapper.selectList(any())).thenReturn(List.of(item));
        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());
        when(apsClient.post(anyString(), any(), eq(Map.class))).thenReturn(Map.of("status", "ok"));

        ApsSyncResultVO result = masterDataSyncService.syncBoms();

        assertEquals(SyncStatus.SUCCESS.getCode(), result.getStatus());
        assertEquals(1, result.getTotalCount());
        verify(apsClient).post(eq("/api/mes/master-data/boms"), argThat(payload -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) payload;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) map.get("data");
            return data.size() == 1 && data.get(0).containsKey("items");
        }), eq(Map.class));

        System.out.println("✓ 1.3 BOM同步成功: 1条BOM（含明细）");
    }

    @Test
    @Order(4)
    @DisplayName("1.4 物料主数据同步 - 正常流程")
    void testSyncMaterials_Success() {
        Material m = new Material();
        m.setMaterialCode("MAT-001");
        m.setMaterialName("钛合金棒料");
        m.setMaterialType("RAW");
        m.setBaseUnit("PCS");
        m.setCategoryLevel1("金属材料");
        m.setCategoryLevel2("钛合金");
        m.setTraceMode("BATCH");

        when(materialMapper.selectList(any())).thenReturn(List.of(m));
        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());
        when(apsClient.post(anyString(), any(), eq(Map.class))).thenReturn(Map.of("status", "ok"));

        ApsSyncResultVO result = masterDataSyncService.syncMaterials();

        assertEquals(SyncStatus.SUCCESS.getCode(), result.getStatus());
        assertEquals(1, result.getTotalCount());

        System.out.println("✓ 1.4 物料主数据同步成功: 1条数据");
    }

    @Test
    @Order(5)
    @DisplayName("1.5 班组信息同步 - 正常流程")
    void testSyncTeams_Success() {
        ProductionTeam t = new ProductionTeam();
        t.setTeamCode("TEAM-A01");
        t.setTeamName("甲班一组");
        t.setOrgCode("ORG-01");
        t.setOrgName("机加车间");
        t.setEnabled(1);

        when(teamMapper.selectList(any())).thenReturn(List.of(t));
        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());
        when(apsClient.post(anyString(), any(), eq(Map.class))).thenReturn(Map.of("status", "ok"));

        ApsSyncResultVO result = masterDataSyncService.syncTeams();

        assertEquals(SyncStatus.SUCCESS.getCode(), result.getStatus());
        assertEquals(1, result.getTotalCount());

        System.out.println("✓ 1.5 班组同步成功: 1条数据");
    }

    @Test
    @Order(6)
    @DisplayName("1.6 主数据全量同步 - 全部类型")
    void testSyncAllMasterData() {
        when(workCenterMapper.selectList(any())).thenReturn(List.of(new WorkCenter()));
        when(processInfoMapper.selectList(any())).thenReturn(List.of(new ProcessInfo()));
        when(bomMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(materialMapper.selectList(any())).thenReturn(List.of(new Material()));
        when(teamMapper.selectList(any())).thenReturn(List.of(new ProductionTeam()));
        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());
        when(apsClient.post(anyString(), any(), eq(Map.class))).thenReturn(Map.of("status", "ok"));

        ApsSyncResultVO result = masterDataSyncService.syncAllMasterData();

        assertNotNull(result);
        assertTrue(result.getTotalCount() >= 0);

        System.out.println("✓ 1.6 主数据全量同步成功");
    }

    @Test
    @Order(7)
    @DisplayName("1.7 工作中心同步 - APS不可用时熔断")
    void testSyncWorkCenters_ApsUnavailable() {
        when(workCenterMapper.selectList(any())).thenReturn(List.of(new WorkCenter()));
        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());
        when(apsClient.post(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("APS服务不可用"));

        ApsSyncResultVO result = masterDataSyncService.syncWorkCenters();

        assertEquals(SyncStatus.FAIL.getCode(), result.getStatus());
        verify(syncLogService).completeLog(any(), eq(0), eq(0), eq(0), anyString());

        System.out.println("✓ 1.7 APS不可用时正确返回失败状态");
    }

    // ==================== 2. 执行反馈测试 ====================

    @Test
    @Order(10)
    @DisplayName("2.1 派工分配结果反馈")
    void testFeedbackDispatchAssignment() {
        DispatchTask task = new DispatchTask();
        task.setWorkOrderId(1001L);
        task.setWorkOrderTaskId(2001L);
        task.setOrderNo("ORD-2024-001");
        task.setProcessNo("OP-010");
        task.setPlanWorkCenterId(5L);

        DispatchAssignment assignment = new DispatchAssignment();
        assignment.setAssignType("DEVICE");
        assignment.setAssigneeCode("CNC-001");
        assignment.setAssigneeName("CNC加工中心1号");
        assignment.setAssignedQty(new BigDecimal("50"));
        assignment.setStatus("ACTIVE");
        assignment.setAssignedTime(LocalDateTime.now());

        when(dispatchTaskMapper.selectById(100L)).thenReturn(task);
        when(dispatchAssignmentMapper.selectById(200L)).thenReturn(assignment);

        feedbackService.feedbackDispatchAssignment(100L, 200L);

        verify(upstreamSyncService).enqueue(
                eq(SyncType.DISPATCH.getCode()),
                eq("DISPATCH_ASSIGNMENT"),
                eq(200L),
                eq("ORD-2024-001"),
                eq(3),
                argThat(json -> json.contains("CNC-001") && json.contains("DEVICE")));

        System.out.println("✓ 2.1 派工分配反馈成功");
    }

    @Test
    @Order(11)
    @DisplayName("2.2 开工检查失败反馈 - 高优先级")
    void testFeedbackStartCheck_Failed() {
        feedbackService.feedbackStartCheck(2001L, "FAILED");

        verify(upstreamSyncService).enqueue(
                eq(SyncType.START_CHECK.getCode()),
                eq("WORK_START_CHECK"),
                eq(2001L),
                isNull(),
                eq(2),  // FAILED 优先级为2
                argThat(json -> json.contains("FAILED")));

        System.out.println("✓ 2.2 开工检查失败反馈成功（优先级=2）");
    }

    @Test
    @Order(12)
    @DisplayName("2.3 开工检查通过反馈 - 普通优先级")
    void testFeedbackStartCheck_Passed() {
        feedbackService.feedbackStartCheck(2001L, "PASSED");

        verify(upstreamSyncService).enqueue(
                eq(SyncType.START_CHECK.getCode()),
                eq("WORK_START_CHECK"),
                eq(2001L),
                isNull(),
                eq(5),  // PASSED 优先级为5
                argThat(json -> json.contains("PASSED")));

        System.out.println("✓ 2.3 开工检查通过反馈成功（优先级=5）");
    }

    @Test
    @Order(13)
    @DisplayName("2.4 工单约束关系反馈")
    void testFeedbackWorkOrderConstraint() {
        WorkOrder wo = new WorkOrder();
        wo.setWorkOrderNo("WO-2024-001");

        WorkOrderConstraint c1 = new WorkOrderConstraint();
        c1.setConstraintType("FINISH_TO_START");
        c1.setRelatedWorkOrderId(1002L);
        c1.setRemark("叶片完成后装配");

        when(workOrderMapper.selectById(1001L)).thenReturn(wo);
        when(constraintMapper.selectList(any())).thenReturn(List.of(c1));

        feedbackService.feedbackWorkOrderConstraint(1001L);

        verify(upstreamSyncService).enqueue(
                eq(SyncType.CONSTRAINT.getCode()),
                eq("WORK_ORDER_CONSTRAINT"),
                eq(1001L),
                eq("WO-2024-001"),
                eq(4),
                argThat(json -> json.contains("FINISH_TO_START")));

        System.out.println("✓ 2.4 工单约束反馈成功");
    }

    @Test
    @Order(14)
    @DisplayName("2.5 交班实际产出反馈")
    void testFeedbackShiftOutput() {
        ShiftHandover handover = new ShiftHandover();
        handover.setProjectName("CFM56叶片加工");
        handover.setProductSerialNo("SN-001");
        handover.setHandoverDate(LocalDate.of(2024, 6, 15));
        handover.setHandoverShift("白班");
        handover.setHandoverTeamName("甲班一组");
        handover.setPlanQty(new BigDecimal("50"));
        handover.setActualQty(new BigDecimal("45"));
        handover.setGapAnalysis("设备临时维修导致停工30分钟");

        when(shiftHandoverMapper.selectById(301L)).thenReturn(handover);

        feedbackService.feedbackShiftOutput(301L);

        verify(upstreamSyncService).enqueue(
                eq(SyncType.SHIFT_OUTPUT.getCode()),
                eq("SHIFT_HANDOVER"),
                eq(301L),
                eq("SN-001"),
                eq(3),
                argThat(json -> json.contains("50") && json.contains("45")));

        System.out.println("✓ 2.5 交班产出反馈成功（计划50，实际45）");
    }

    @Test
    @Order(15)
    @DisplayName("2.6 物料短缺反馈 - 存在缺料")
    void testFeedbackMaterialShortage_HasShortage() {
        WorkOrder wo = new WorkOrder();
        wo.setWorkOrderNo("WO-2024-001");

        WorkOrderInputMaterial m1 = new WorkOrderInputMaterial();
        m1.setMaterialCode("MAT-001");
        m1.setMaterialName("钛合金棒料");
        m1.setRequiredQty(new BigDecimal("100"));
        m1.setIssuedQty(new BigDecimal("60"));

        WorkOrderInputMaterial m2 = new WorkOrderInputMaterial();
        m2.setMaterialCode("MAT-002");
        m2.setMaterialName("紧固件");
        m2.setRequiredQty(new BigDecimal("50"));
        m2.setIssuedQty(new BigDecimal("50"));  // 已齐套

        when(workOrderMapper.selectById(1001L)).thenReturn(wo);
        when(inputMaterialMapper.selectList(any())).thenReturn(List.of(m1, m2));

        feedbackService.feedbackMaterialShortage(1001L);

        verify(upstreamSyncService).enqueue(
                eq(SyncType.MATERIAL_SHORTAGE.getCode()),
                eq("MATERIAL_SHORTAGE"),
                eq(1001L),
                eq("WO-2024-001"),
                eq(2),
                argThat(json -> json.contains("MAT-001") && json.contains("40") && !json.contains("MAT-002")));

        System.out.println("✓ 2.6 物料短缺反馈成功（MAT-001缺40，MAT-002齐套不报）");
    }

    @Test
    @Order(16)
    @DisplayName("2.7 物料短缺反馈 - 全部齐套不发送")
    void testFeedbackMaterialShortage_NoShortage() {
        WorkOrder wo = new WorkOrder();
        wo.setWorkOrderNo("WO-2024-002");

        WorkOrderInputMaterial m1 = new WorkOrderInputMaterial();
        m1.setRequiredQty(new BigDecimal("50"));
        m1.setIssuedQty(new BigDecimal("50"));

        when(workOrderMapper.selectById(1002L)).thenReturn(wo);
        when(inputMaterialMapper.selectList(any())).thenReturn(List.of(m1));

        feedbackService.feedbackMaterialShortage(1002L);

        verify(upstreamSyncService, never()).enqueue(anyString(), anyString(),
                anyLong(), anyString(), anyInt(), anyString());

        System.out.println("✓ 2.7 全部齐套时不发送短缺反馈");
    }

    @Test
    @Order(17)
    @DisplayName("2.8 领料进度反馈")
    void testFeedbackRequisitionProgress() {
        MaterialRequisition req = new MaterialRequisition();
        req.setRequisitionNo("REQ-2024-001");
        req.setWorkOrderNo("WO-2024-001");
        req.setProductCode("PROD-001");
        req.setPlanQty(new BigDecimal("100"));
        req.setActualQty(new BigDecimal("80"));
        req.setQualifiedQty(new BigDecimal("78"));
        req.setStatus("PARTIAL_ISSUED");

        when(requisitionMapper.selectById(401L)).thenReturn(req);

        feedbackService.feedbackRequisitionProgress(401L);

        verify(upstreamSyncService).enqueue(
                eq(SyncType.REQUISITION.getCode()),
                eq("MATERIAL_REQUISITION"),
                eq(401L),
                eq("REQ-2024-001"),
                eq(4),
                argThat(json -> json.contains("PARTIAL_ISSUED")));

        System.out.println("✓ 2.8 领料进度反馈成功");
    }

    @Test
    @Order(18)
    @DisplayName("2.9 供应计划完成度反馈")
    void testFeedbackSupplyProgress() {
        WorkOrder wo = new WorkOrder();
        wo.setWorkOrderNo("WO-2024-001");

        WorkOrderSupplyPlan plan = new WorkOrderSupplyPlan();
        plan.setDemandPlanNo("DEM-001");
        plan.setSupplyPlanNo("SUP-001");
        plan.setSupplyQty(new BigDecimal("100"));
        plan.setCompletedQty(new BigDecimal("75"));

        when(workOrderMapper.selectById(1001L)).thenReturn(wo);
        when(supplyPlanMapper.selectById(501L)).thenReturn(plan);

        feedbackService.feedbackSupplyProgress(1001L, 501L);

        verify(upstreamSyncService).enqueue(
                eq(SyncType.SUPPLY_PROGRESS.getCode()),
                eq("SUPPLY_PLAN"),
                eq(501L),
                eq("SUP-001"),
                eq(4),
                argThat(json -> json.contains("75")));

        System.out.println("✓ 2.9 供应计划完成度反馈成功（100计划，75完成）");
    }

    @Test
    @Order(19)
    @DisplayName("2.10 工单状态变更实时反馈")
    void testFeedbackWorkOrderStatusChange() {
        WorkOrder wo = new WorkOrder();
        wo.setWorkOrderNo("WO-2024-001");
        wo.setOrderPlanNo("ORD-2024-001");
        wo.setProductCode("PROD-001");
        wo.setPlanStartTime(LocalDateTime.of(2024, 6, 15, 8, 0));
        wo.setPlanEndTime(LocalDateTime.of(2024, 6, 15, 17, 0));
        wo.setActualStartTime(LocalDateTime.of(2024, 6, 15, 8, 15));
        wo.setActualEndTime(LocalDateTime.of(2024, 6, 15, 16, 30));

        when(workOrderMapper.selectById(1001L)).thenReturn(wo);

        feedbackService.feedbackWorkOrderStatusChange(1001L, "IN_PROGRESS", "COMPLETED");

        verify(upstreamSyncService).enqueue(
                eq(SyncType.STATUS_CHANGE.getCode()),
                eq("WORK_ORDER"),
                eq(1001L),
                eq("WO-2024-001"),
                eq(2),
                argThat(json -> json.contains("IN_PROGRESS") && json.contains("COMPLETED")));

        System.out.println("✓ 2.10 工单状态变更反馈成功（IN_PROGRESS→COMPLETED）");
    }

    @Test
    @Order(20)
    @DisplayName("2.11 工艺变更通知反馈")
    void testFeedbackProcessChange() {
        feedbackService.feedbackProcessChange("BOM_UPGRADE", 101L, "BOM-2024-001");

        verify(upstreamSyncService).enqueue(
                eq(SyncType.PROCESS_CHANGE.getCode()),
                eq("BOM_UPGRADE"),
                eq(101L),
                eq("BOM-2024-001"),
                eq(3),
                argThat(json -> json.contains("BOM_UPGRADE")));

        System.out.println("✓ 2.11 工艺变更通知反馈成功");
    }

    // ==================== 3. APS下发回调测试 ====================

    @Test
    @Order(30)
    @DisplayName("3.1 MRP结果接收")
    void testHandleMrpResult() {
        ApsMrpCallbackVO mrp = new ApsMrpCallbackVO();
        mrp.setRequestId("mrp-001");
        mrp.setScheduleBatchId("BATCH-001");

        ApsMrpCallbackVO.MrpItem item = new ApsMrpCallbackVO.MrpItem();
        item.setWorkOrderNo("WO-2024-001");
        item.setMaterialCode("MAT-001");
        item.setRequiredQty(new BigDecimal("100"));
        item.setUnit("PCS");
        item.setPriority(1);
        mrp.setItems(List.of(item));

        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());

        callbackService.handleMrpResult(mrp);

        verify(syncLogService).createLog(eq("mrp-001"),
                eq(SyncDirection.DOWNSTREAM.getCode()), eq(SyncType.MRP.getCode()));
        verify(syncLogService).completeLog(any(), eq(1), eq(1), eq(0), isNull());

        System.out.println("✓ 3.1 MRP结果接收成功: 1条物料需求");
    }

    @Test
    @Order(31)
    @DisplayName("3.2 资源分配计划接收 - 自动更新派工")
    void testHandleResourceAllocation() {
        ApsResourceAllocationVO allocation = new ApsResourceAllocationVO();
        allocation.setRequestId("alloc-001");
        allocation.setScheduleBatchId("BATCH-001");

        ApsResourceAllocationVO.AllocationItem item = new ApsResourceAllocationVO.AllocationItem();
        item.setWorkOrderNo("WO-2024-001");
        item.setProcessNo("OP-010");
        item.setWorkCenterCode("WC-001");
        item.setAssignType("DEVICE");
        item.setAssigneeCode("CNC-001");
        item.setAssigneeName("CNC加工中心1号");
        item.setAssignedQty(new BigDecimal("50"));
        item.setPlanStartTime(LocalDateTime.of(2024, 6, 15, 8, 0));
        item.setPlanEndTime(LocalDateTime.of(2024, 6, 15, 12, 0));
        allocation.setItems(List.of(item));

        DispatchTask task = new DispatchTask();
        task.setId(100L);
        task.setOrderNo("WO-2024-001");
        task.setProcessNo("OP-010");

        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());
        when(dispatchTaskMapper.selectOne(any())).thenReturn(task);
        when(dispatchTaskMapper.updateById(any())).thenReturn(1);
        when(dispatchAssignmentMapper.insert(any())).thenReturn(1);

        callbackService.handleResourceAllocation(allocation);

        verify(dispatchTaskMapper).updateById(argThat(t -> {
            DispatchTask dt = (DispatchTask) t;
            return dt.getPlanStartTime() != null && dt.getPlanEndTime() != null;
        }));
        verify(dispatchAssignmentMapper).insert(argThat(a -> {
            DispatchAssignment da = (DispatchAssignment) a;
            return "CNC-001".equals(da.getAssigneeCode()) && "APS".equals(da.getAssignedBy());
        }));

        System.out.println("✓ 3.2 资源分配接收成功: 派工任务时间已更新，分配记录已创建");
    }

    @Test
    @Order(32)
    @DisplayName("3.3 甘特图数据接收")
    void testHandleGanttData() {
        ApsGanttDataVO gantt = new ApsGanttDataVO();
        gantt.setRequestId("gantt-001");
        gantt.setScheduleBatchId("BATCH-001");
        gantt.setRangeStart(LocalDateTime.of(2024, 6, 15, 0, 0));
        gantt.setRangeEnd(LocalDateTime.of(2024, 6, 22, 0, 0));

        ApsGanttDataVO.GanttTask task = new ApsGanttDataVO.GanttTask();
        task.setTaskId("TASK-001");
        task.setWorkOrderNo("WO-2024-001");
        task.setProcessName("粗车加工");
        task.setResourceCode("WC-001");
        task.setDuration(240);
        task.setPredecessors(Collections.emptyList());
        gantt.setTasks(List.of(task));

        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());

        callbackService.handleGanttData(gantt);

        verify(syncLogService).completeLog(any(), eq(1), eq(1), eq(0), isNull());

        System.out.println("✓ 3.3 甘特图数据接收成功: 1条任务，时间范围6/15-6/22");
    }

    @Test
    @Order(33)
    @DisplayName("3.4 产能负荷数据接收")
    void testHandleCapacityLoad() {
        ApsCapacityLoadVO capacity = new ApsCapacityLoadVO();
        capacity.setRequestId("cap-001");
        capacity.setScheduleBatchId("BATCH-001");
        capacity.setCalculatedAt(LocalDateTime.now());

        ApsCapacityLoadVO.CapacityItem item1 = new ApsCapacityLoadVO.CapacityItem();
        item1.setWorkCenterCode("WC-001");
        item1.setWorkCenterName("CNC加工中心1号");
        item1.setDate(LocalDate.of(2024, 6, 15));
        item1.setAvailableCapacity(new BigDecimal("480"));
        item1.setScheduledCapacity(new BigDecimal("420"));
        item1.setLoadRate(new BigDecimal("87.50"));
        item1.setOverloaded(false);

        ApsCapacityLoadVO.CapacityItem item2 = new ApsCapacityLoadVO.CapacityItem();
        item2.setWorkCenterCode("WC-002");
        item2.setLoadRate(new BigDecimal("105.00"));
        item2.setOverloaded(true);  // 超负荷
        capacity.setItems(List.of(item1, item2));

        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());

        callbackService.handleCapacityLoad(capacity);

        verify(syncLogService).completeLog(any(), eq(2), eq(2), eq(0), isNull());

        System.out.println("✓ 3.4 产能负荷接收成功: WC-001(87.5%), WC-002(105%超负荷)");
    }

    @Test
    @Order(34)
    @DisplayName("3.5 排程变更通知接收 - 时间变更自动更新工单")
    void testHandleScheduleChange_TimeChanged() {
        ApsScheduleChangeVO change = new ApsScheduleChangeVO();
        change.setRequestId("chg-001");
        change.setScheduleBatchId("BATCH-001");
        change.setChangeReason("设备故障导致重排");
        change.setChangeTime(LocalDateTime.now());

        ApsScheduleChangeVO.AffectedOrder affected = new ApsScheduleChangeVO.AffectedOrder();
        affected.setWorkOrderNo("WO-2024-001");
        affected.setChangeType("TIME_CHANGED");
        affected.setOldStartTime(LocalDateTime.of(2024, 6, 15, 8, 0));
        affected.setNewStartTime(LocalDateTime.of(2024, 6, 15, 13, 0));
        affected.setOldEndTime(LocalDateTime.of(2024, 6, 15, 12, 0));
        affected.setNewEndTime(LocalDateTime.of(2024, 6, 15, 17, 0));
        affected.setRemark("因CNC-001设备故障，推迟到下午");
        change.setAffectedOrders(List.of(affected));

        WorkOrder wo = new WorkOrder();
        wo.setWorkOrderNo("WO-2024-001");
        wo.setPlanStartTime(LocalDateTime.of(2024, 6, 15, 8, 0));
        wo.setPlanEndTime(LocalDateTime.of(2024, 6, 15, 12, 0));

        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());
        when(workOrderMapper.selectOne(any())).thenReturn(wo);
        when(workOrderMapper.updateById(any())).thenReturn(1);

        callbackService.handleScheduleChange(change);

        verify(workOrderMapper).updateById(argThat(w -> {
            WorkOrder updated = (WorkOrder) w;
            return updated.getPlanStartTime().getHour() == 13
                    && updated.getPlanEndTime().getHour() == 17;
        }));

        System.out.println("✓ 3.5 排程变更接收成功: 工单时间从8:00-12:00更新为13:00-17:00");
    }

    @Test
    @Order(35)
    @DisplayName("3.6 排程变更 - 空受影响列表")
    void testHandleScheduleChange_Empty() {
        ApsScheduleChangeVO change = new ApsScheduleChangeVO();
        change.setRequestId("chg-002");
        change.setAffectedOrders(null);

        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());

        callbackService.handleScheduleChange(change);

        verify(workOrderMapper, never()).selectOne(any());
        verify(syncLogService).completeLog(any(), eq(0), eq(0), eq(0), isNull());

        System.out.println("✓ 3.6 空变更列表正常处理（不报错）");
    }

    // ==================== 4. SyncType 枚举完整性测试 ====================

    @Test
    @Order(40)
    @DisplayName("4.1 SyncType 枚举完整性检查")
    void testSyncTypeEnum() {
        // 验证原有类型
        assertNotNull(SyncType.valueOf("ORDER"));
        assertNotNull(SyncType.valueOf("WORKORDER"));
        assertNotNull(SyncType.valueOf("INVENTORY"));
        assertNotNull(SyncType.valueOf("QUALITY"));
        assertNotNull(SyncType.valueOf("ABNORMAL"));
        assertNotNull(SyncType.valueOf("OUTSOURCE"));
        assertNotNull(SyncType.valueOf("TRANSFER"));

        // 验证新增主数据类型
        assertNotNull(SyncType.valueOf("WORK_CENTER"));
        assertNotNull(SyncType.valueOf("PROCESS_ROUTE"));
        assertNotNull(SyncType.valueOf("BOM"));
        assertNotNull(SyncType.valueOf("MATERIAL_MASTER"));
        assertNotNull(SyncType.valueOf("TEAM"));

        // 验证新增执行反馈类型
        assertNotNull(SyncType.valueOf("DISPATCH"));
        assertNotNull(SyncType.valueOf("START_CHECK"));
        assertNotNull(SyncType.valueOf("CONSTRAINT"));
        assertNotNull(SyncType.valueOf("SHIFT_OUTPUT"));
        assertNotNull(SyncType.valueOf("MATERIAL_SHORTAGE"));
        assertNotNull(SyncType.valueOf("REQUISITION"));
        assertNotNull(SyncType.valueOf("SUPPLY_PROGRESS"));
        assertNotNull(SyncType.valueOf("STATUS_CHANGE"));
        assertNotNull(SyncType.valueOf("PROCESS_CHANGE"));

        // 验证新增APS下发类型
        assertNotNull(SyncType.valueOf("MRP"));
        assertNotNull(SyncType.valueOf("RESOURCE_ALLOCATION"));
        assertNotNull(SyncType.valueOf("GANTT"));
        assertNotNull(SyncType.valueOf("CAPACITY_LOAD"));
        assertNotNull(SyncType.valueOf("SCHEDULE_CHANGE"));

        assertEquals(29, SyncType.values().length);

        System.out.println("✓ 4.1 SyncType枚举完整: 共29个类型（10原有+5主数据+9执行反馈+5APS下发）");
    }

    @Test
    @Order(41)
    @DisplayName("4.2 VO序列化/反序列化测试")
    void testVoSerialization() throws Exception {
        // MRP VO
        ApsMrpCallbackVO mrp = new ApsMrpCallbackVO();
        mrp.setRequestId("test-001");
        mrp.setItems(new ArrayList<>());
        String mrpJson = objectMapper.writeValueAsString(mrp);
        ApsMrpCallbackVO parsed = objectMapper.readValue(mrpJson, ApsMrpCallbackVO.class);
        assertEquals("test-001", parsed.getRequestId());

        // Gantt VO
        ApsGanttDataVO gantt = new ApsGanttDataVO();
        gantt.setRequestId("test-002");
        gantt.setTasks(new ArrayList<>());
        String ganttJson = objectMapper.writeValueAsString(gantt);
        assertNotNull(objectMapper.readValue(ganttJson, ApsGanttDataVO.class));

        // Capacity VO
        ApsCapacityLoadVO cap = new ApsCapacityLoadVO();
        cap.setRequestId("test-003");
        cap.setItems(new ArrayList<>());
        String capJson = objectMapper.writeValueAsString(cap);
        assertNotNull(objectMapper.readValue(capJson, ApsCapacityLoadVO.class));

        // Schedule Change VO
        ApsScheduleChangeVO chg = new ApsScheduleChangeVO();
        chg.setRequestId("test-004");
        chg.setAffectedOrders(new ArrayList<>());
        String chgJson = objectMapper.writeValueAsString(chg);
        assertNotNull(objectMapper.readValue(chgJson, ApsScheduleChangeVO.class));

        // Resource Allocation VO
        ApsResourceAllocationVO alloc = new ApsResourceAllocationVO();
        alloc.setRequestId("test-005");
        alloc.setItems(new ArrayList<>());
        String allocJson = objectMapper.writeValueAsString(alloc);
        assertNotNull(objectMapper.readValue(allocJson, ApsResourceAllocationVO.class));

        System.out.println("✓ 4.2 全部5个VO序列化/反序列化通过");
    }

    // ==================== 辅助方法 ====================

    private ApsSyncLog buildMockSyncLog() {
        ApsSyncLog log = new ApsSyncLog();
        log.setId(1L);
        log.setBatchId(UUID.randomUUID().toString());
        return log;
    }
}
