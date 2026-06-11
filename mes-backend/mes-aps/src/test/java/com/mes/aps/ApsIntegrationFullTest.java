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
import com.mes.dispatch.enums.DispatchStatus;
import com.mes.dispatch.mapper.DispatchAssignmentMapper;
import com.mes.dispatch.mapper.DispatchTaskMapper;
import com.mes.dispatch.service.IDispatchTaskService;
import com.mes.material.domain.entity.MaterialRequisition;
import com.mes.material.mapper.MaterialRequisitionMapper;
import com.mes.material.service.IMaterialRequisitionService;
import com.mes.plan.mapper.OrderPlanMapper;
import com.mes.process.domain.entity.ManufacturingBom;
import com.mes.process.domain.entity.ManufacturingBomItem;
import com.mes.process.domain.entity.ProcessInfo;
import com.mes.process.domain.entity.Route;
import com.mes.process.domain.entity.RouteStep;
import com.mes.process.mapper.ManufacturingBomItemMapper;
import com.mes.process.mapper.ManufacturingBomMapper;
import com.mes.process.mapper.ProcessInfoMapper;
import com.mes.process.mapper.RouteMapper;
import com.mes.process.mapper.RouteStepMapper;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

    private static final String FEEDBACK_DISPATCH = "DISPATCH";
    private static final String FEEDBACK_START_CHECK = "START_CHECK";
    private static final String FEEDBACK_CONSTRAINT = "CONSTRAINT";
    private static final String FEEDBACK_SHIFT_OUTPUT = "SHIFT_OUTPUT";
    private static final String FEEDBACK_MATERIAL_SHORTAGE = "MATERIAL_SHORTAGE";
    private static final String FEEDBACK_REQUISITION = "REQUISITION";
    private static final String FEEDBACK_SUPPLY_PROGRESS = "SUPPLY_PROGRESS";
    private static final String FEEDBACK_STATUS_CHANGE = "STATUS_CHANGE";
    private static final String FEEDBACK_PROCESS_CHANGE = "PROCESS_CHANGE";

    private ObjectMapper objectMapper;

    // ===== 主数据同步依赖 =====
    @Mock private ApsClient apsClient;
    @Mock private IApsSyncConfigService configService;
    @Mock private IApsSyncLogService syncLogService;
    @Mock private WorkCenterMapper workCenterMapper;
    @Mock private ProcessInfoMapper processInfoMapper;
    @Mock private RouteMapper routeMapper;
    @Mock private RouteStepMapper routeStepMapper;
    @Mock private ManufacturingBomMapper bomMapper;
    @Mock private ManufacturingBomItemMapper bomItemMapper;
    @Mock private MaterialMapper materialMapper;
    @Mock private ProductionTeamMapper teamMapper;

    // ===== 执行反馈依赖 =====
    @Mock private IApsUpstreamSyncService upstreamSyncService;
    @Mock private DispatchTaskMapper dispatchTaskMapper;
    @Mock private DispatchAssignmentMapper dispatchAssignmentMapper;
    @Mock private IDispatchTaskService dispatchTaskService;
    @Mock private ShiftHandoverMapper shiftHandoverMapper;
    @Mock private WorkOrderMapper workOrderMapper;
    @Mock private WorkOrderInputMaterialMapper inputMaterialMapper;
    @Mock private WorkOrderConstraintMapper constraintMapper;
    @Mock private WorkOrderSupplyPlanMapper supplyPlanMapper;
    @Mock private MaterialRequisitionMapper requisitionMapper;
    @Mock private IMaterialRequisitionService materialRequisitionService;
    @Mock private ApsSyncQueueMapper syncQueueMapper;
    @Mock private OrderPlanMapper orderPlanMapper;
    // P2 升级后 ApsExtendedCallbackServiceImpl 新增了幂等服务依赖
    @Mock private com.mes.aps.service.ApsCallbackIdempotencyService idempotencyService;
    private RecordingInvocationHandler ganttCacheMapperHandler;
    private RecordingInvocationHandler capacityLoadMapperHandler;
    private Object ganttCacheMapper;
    private Object capacityLoadMapper;

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
                workCenterMapper, processInfoMapper, routeMapper, routeStepMapper, bomMapper, bomItemMapper,
                materialMapper, teamMapper);

        feedbackService = new ApsExecutionFeedbackServiceImpl(
                upstreamSyncService, syncLogService, objectMapper,
                dispatchTaskMapper, dispatchAssignmentMapper, shiftHandoverMapper,
                workOrderMapper, inputMaterialMapper, constraintMapper,
                supplyPlanMapper, requisitionMapper);

        ganttCacheMapper = createOptionalRecorderProxy(
                "com.mes.aps.mapper.ApsGanttCacheMapper",
                handler -> ganttCacheMapperHandler = handler);
        capacityLoadMapper = createOptionalRecorderProxy(
                "com.mes.aps.mapper.ApsCapacityLoadMapper",
                handler -> capacityLoadMapperHandler = handler);

        callbackService = instantiateCallbackService();

        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());

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
        wc1.setResourceSubtype("FIVE_AXIS");
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
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(apsClient).post(eq("/api/mes/master-data/work-centers"), payloadCaptor.capture(), eq(Map.class));
        List<?> data = (List<?>) payloadCaptor.getValue().get("data");
        Map<?, ?> first = (Map<?, ?>) data.get(0);
        assertEquals("FIVE_AXIS", first.get("resourceSubtype"));
        assertFalse(first.containsKey("furnaceResourceType"));
        verify(syncLogService).completeLog(any(), eq(2), eq(2), eq(0), isNull());

        System.out.println("✓ 1.1 工作中心同步成功: 2条数据");
    }

    @Test
    @Order(2)
    @DisplayName("1.2 工艺路线同步 - 正常流程")
    void testSyncProcessRoutes_Success() {
        Route route = new Route();
        route.setId(101L);
        route.setRouteCode("ROUTE-001");
        route.setRouteName("CFM56叶片标准路线");
        route.setProductCode("PROD-001");
        route.setProductCategory("BLADE");
        route.setMachineModel("CFM56");
        route.setProductType("NEW");
        route.setStatus("ACTIVE");

        RouteStep step20 = new RouteStep();
        step20.setId(1002L);
        step20.setRouteId(101L);
        step20.setSequenceNo(20);
        step20.setProcessId(202L);
        step20.setProcessNo("OP-020");
        step20.setProcessName("精车加工");
        step20.setWorkCenterId(2L);
        step20.setHandleTime(new BigDecimal("60"));
        step20.setPredecessorStepId(1001L);
        step20.setParallelFlag(0);
        step20.setOptionalFlag(0);

        RouteStep step10 = new RouteStep();
        step10.setId(1001L);
        step10.setRouteId(101L);
        step10.setSequenceNo(10);
        step10.setProcessId(201L);
        step10.setProcessNo("OP-010");
        step10.setProcessName("粗车加工");
        step10.setWorkCenterId(1L);
        step10.setHandleTime(new BigDecimal("120"));
        step10.setParallelFlag(0);
        step10.setOptionalFlag(0);

        WorkCenter wc1 = new WorkCenter();
        wc1.setId(1L);
        wc1.setWorkCenterCode("WC-001");
        wc1.setWorkCenterName("CNC加工中心1号");

        WorkCenter wc2 = new WorkCenter();
        wc2.setId(2L);
        wc2.setWorkCenterCode("WC-002");
        wc2.setWorkCenterName("CNC加工中心2号");

        when(routeMapper.selectList(any())).thenReturn(List.of(route));
        when(routeStepMapper.selectList(any())).thenReturn(List.of(step20, step10));
        when(workCenterMapper.selectById(1L)).thenReturn(wc1);
        when(workCenterMapper.selectById(2L)).thenReturn(wc2);
        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());
        when(apsClient.post(anyString(), any(), eq(Map.class))).thenReturn(Map.of("status", "ok"));

        ApsSyncResultVO result = masterDataSyncService.syncProcessRoutes();

        assertNotNull(result);
        assertEquals(SyncStatus.SUCCESS.getCode(), result.getStatus());
        assertEquals(2, result.getTotalCount());
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(apsClient).post(eq("/api/mes/master-data/process-routes"), payloadCaptor.capture(), eq(Map.class));
        List<?> data = (List<?>) payloadCaptor.getValue().get("data");
        assertEquals(2, data.size());
        Map<?, ?> first = (Map<?, ?>) data.get(0);
        Map<?, ?> second = (Map<?, ?>) data.get(1);
        assertEquals("ROUTE-001", first.get("routeCode"));
        assertEquals("PROD-001", first.get("productCode"));
        assertEquals(10, first.get("sequenceNo"));
        assertEquals(10, first.get("processSequence"));
        assertEquals("WC-001", first.get("workCenterCode"));
        assertEquals("WC-001", first.get("resourceCode"));
        assertEquals(120D, first.get("cycleTime"));
        assertEquals(List.of(), first.get("dependencySequenceNos"));
        assertEquals(20, second.get("sequenceNo"));
        assertEquals(10, second.get("predecessorSequenceNo"));
        assertEquals(List.of(10), second.get("dependencySequenceNos"));
        assertEquals("WC-002", second.get("workCenterCode"));

        System.out.println("✓ 1.2 工艺路线同步成功: 1条路线展开为2个有序步骤");
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

        verifyUnsupportedFeedbackAudited(FEEDBACK_DISPATCH);

        System.out.println("✓ 2.1 派工分配反馈改为本地审计失败记录");
    }

    @Test
    @Order(11)
    @DisplayName("2.2 开工检查失败反馈 - 高优先级")
    void testFeedbackStartCheck_Failed() {
        feedbackService.feedbackStartCheck(2001L, "FAILED");

        verifyUnsupportedFeedbackAudited(FEEDBACK_START_CHECK);

        System.out.println("✓ 2.2 开工检查失败反馈改为本地审计失败记录");
    }

    @Test
    @Order(12)
    @DisplayName("2.3 开工检查通过反馈 - 普通优先级")
    void testFeedbackStartCheck_Passed() {
        feedbackService.feedbackStartCheck(2001L, "PASSED");

        verifyUnsupportedFeedbackAudited(FEEDBACK_START_CHECK);

        System.out.println("✓ 2.3 开工检查通过反馈改为本地审计失败记录");
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

        verifyUnsupportedFeedbackAudited(FEEDBACK_CONSTRAINT);

        System.out.println("✓ 2.4 工单约束反馈改为本地审计失败记录");
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

        verifyUnsupportedFeedbackAudited(FEEDBACK_SHIFT_OUTPUT);

        System.out.println("✓ 2.5 交班产出反馈改为本地审计失败记录");
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

        verifyUnsupportedFeedbackAudited(FEEDBACK_MATERIAL_SHORTAGE);

        System.out.println("✓ 2.6 物料短缺反馈改为本地审计失败记录");
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
        verify(syncLogService, never()).createLog(anyString(), eq(SyncDirection.UPSTREAM.getCode()),
                eq(FEEDBACK_MATERIAL_SHORTAGE));

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

        verifyUnsupportedFeedbackAudited(FEEDBACK_REQUISITION);

        System.out.println("✓ 2.8 领料进度反馈改为本地审计失败记录");
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

        verifyUnsupportedFeedbackAudited(FEEDBACK_SUPPLY_PROGRESS);

        System.out.println("✓ 2.9 供应计划完成度反馈改为本地审计失败记录");
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

        verifyUnsupportedFeedbackAudited(FEEDBACK_STATUS_CHANGE);

        System.out.println("✓ 2.10 工单状态变更反馈改为本地审计失败记录");
    }

    @Test
    @Order(20)
    @DisplayName("2.11 工艺变更通知反馈")
    void testFeedbackProcessChange() {
        feedbackService.feedbackProcessChange("BOM_UPGRADE", 101L, "BOM-2024-001");

        verifyUnsupportedFeedbackAudited(FEEDBACK_PROCESS_CHANGE);

        System.out.println("✓ 2.11 工艺变更通知反馈改为本地审计失败记录");
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

        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(1001L);
        workOrder.setWorkOrderNo("WO-2024-001");
        workOrder.setProductCode("PROD-001");
        workOrder.setProductName("CFM56叶片");
        workOrder.setPlanQty(new BigDecimal("100"));
        workOrder.setQtyUnit("PCS");

        Material material = new Material();
        material.setId(501L);
        material.setMaterialCode("MAT-001");
        material.setMaterialName("钛合金棒料");
        material.setBaseUnit("PCS");

        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());
        when(workOrderMapper.selectOne(any())).thenReturn(workOrder);
        when(materialMapper.selectOne(any())).thenReturn(material);
        when(materialRequisitionService.createFromMrp(any())).thenReturn(3001L);

        callbackService.handleMrpResult(mrp);

        verify(syncLogService).createLog(eq("mrp-001"),
                eq(SyncDirection.DOWNSTREAM.getCode()), eq(SyncType.MRP.getCode()));
        verify(materialRequisitionService).createFromMrp(argThat(dto ->
                dto.getWorkOrderId() != null
                        && "WO-2024-001".equals(dto.getWorkOrderNo())
                        && dto.getItems() != null
                        && dto.getItems().size() == 1
                        && "MAT-001".equals(dto.getItems().get(0).getMaterialCode())
                        && item.getRequiredQty().compareTo(dto.getItems().get(0).getDemandQty()) == 0));
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
        assertNotNull(ganttCacheMapper, "APS 甘特图缓存 mapper 应存在并用于回调落库");

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
        task.setStartTime(LocalDateTime.of(2024, 6, 15, 8, 0));
        task.setEndTime(LocalDateTime.of(2024, 6, 15, 12, 0));
        task.setDuration(240);
        task.setStatus("SCHEDULED");
        task.setPriority(2);
        task.setPredecessors(List.of("TASK-000"));
        gantt.setTasks(List.of(task));

        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());

        callbackService.handleGanttData(gantt);

        assertEquals(1, ganttCacheMapperHandler.count("delete"));
        assertEquals(1, ganttCacheMapperHandler.count("insert"));
        Object insertedTask = ganttCacheMapperHandler.firstArgument("insert");
        assertEquals("BATCH-001", readField(insertedTask, "scheduleBatchId"));
        assertEquals("TASK-001", readField(insertedTask, "taskId"));
        assertEquals("WO-2024-001", readField(insertedTask, "workOrderNo"));
        assertEquals("WC-001", readField(insertedTask, "resourceCode"));
        assertEquals(LocalDateTime.of(2024, 6, 15, 0, 0), readField(insertedTask, "rangeStart"));
        assertEquals(LocalDateTime.of(2024, 6, 22, 0, 0), readField(insertedTask, "rangeEnd"));
        assertTrue(String.valueOf(readField(insertedTask, "predecessors")).contains("TASK-000"));
        assertNotNull(readField(insertedTask, "createdTime"));
        verify(syncLogService).completeLog(any(), eq(1), eq(1), eq(0), isNull());

        System.out.println("✓ 3.3 甘特图数据接收成功: 1条任务，时间范围6/15-6/22");
    }

    @Test
    @Order(33)
    @DisplayName("3.4 产能负荷数据接收")
    void testHandleCapacityLoad() {
        assertNotNull(capacityLoadMapper, "APS 产能负荷 mapper 应存在并用于回调落库");

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

        assertEquals(1, capacityLoadMapperHandler.count("delete"));
        assertEquals(2, capacityLoadMapperHandler.count("insert"));
        Object firstItem = capacityLoadMapperHandler.argument("insert", 0);
        Object secondItem = capacityLoadMapperHandler.argument("insert", 1);
        assertEquals("BATCH-001", readField(firstItem, "scheduleBatchId"));
        assertEquals("WC-001", readField(firstItem, "workCenterCode"));
        assertEquals(LocalDate.of(2024, 6, 15), readField(firstItem, "loadDate"));
        assertEquals(new BigDecimal("87.50"), readField(firstItem, "loadRate"));
        assertEquals(Boolean.FALSE, readField(firstItem, "overloaded"));
        assertEquals("WC-002", readField(secondItem, "workCenterCode"));
        assertEquals(Boolean.TRUE, readField(secondItem, "overloaded"));
        assertNotNull(readField(firstItem, "createdTime"));
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
    @DisplayName("3.6 排程变更通知接收 - APS取消会撤销未完成派工并清空工单计划时间")
    void testHandleScheduleChange_Cancelled() {
        ApsScheduleChangeVO change = new ApsScheduleChangeVO();
        change.setRequestId("chg-002");
        change.setScheduleBatchId("BATCH-001");
        change.setChangeReason("产能重排取消");

        ApsScheduleChangeVO.AffectedOrder affected = new ApsScheduleChangeVO.AffectedOrder();
        affected.setWorkOrderNo("WO-2024-001");
        affected.setChangeType("CANCELLED");
        affected.setRemark("本批次不再投产");
        change.setAffectedOrders(List.of(affected));

        WorkOrder wo = new WorkOrder();
        wo.setWorkOrderNo("WO-2024-001");
        wo.setPlanStartTime(LocalDateTime.of(2024, 6, 15, 8, 0));
        wo.setPlanEndTime(LocalDateTime.of(2024, 6, 15, 12, 0));
        wo.setRemark("原始备注");

        DispatchTask activeTask = new DispatchTask();
        activeTask.setId(501L);
        activeTask.setDispatchStatus(DispatchStatus.ASSIGNED.getCode());

        DispatchTask completedTask = new DispatchTask();
        completedTask.setId(502L);
        completedTask.setDispatchStatus(DispatchStatus.COMPLETED.getCode());

        when(syncLogService.createLog(anyString(), anyString(), anyString()))
                .thenReturn(buildMockSyncLog());
        when(workOrderMapper.selectOne(any())).thenReturn(wo);
        when(dispatchTaskMapper.selectList(any())).thenReturn(List.of(activeTask, completedTask));
        when(workOrderMapper.updateById(any())).thenReturn(1);

        callbackService.handleScheduleChange(change);

        verify(dispatchTaskService).cancel(eq(501L), argThat(reason ->
                reason.contains("APS取消排程")
                        && reason.contains("产能重排取消")
                        && reason.contains("本批次不再投产")));
        verify(dispatchTaskService, never()).cancel(eq(502L), anyString());
        verify(workOrderMapper).updateById(argThat(updated -> {
            WorkOrder order = (WorkOrder) updated;
            return order.getPlanStartTime() == null
                    && order.getPlanEndTime() == null
                    && order.getRemark() != null
                    && order.getRemark().contains("APS取消排程");
        }));

        System.out.println("✓ 3.6 APS取消排程接收成功: 未完成派工已撤销，工单计划时间已清空");
    }

    @Test
    @Order(36)
    @DisplayName("3.6 排程变更 - 空受影响列表")
    void testHandleScheduleChange_Empty() {
        ApsScheduleChangeVO change = new ApsScheduleChangeVO();
        change.setRequestId("chg-003");
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
    @DisplayName("4.1 SyncType 合同枚举收缩检查")
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

        // 执行反馈类型已下沉为 APS 内部反馈码，不再占用集成合同枚举
        assertTrue(SyncType.fromCode(FEEDBACK_DISPATCH).isEmpty());
        assertTrue(SyncType.fromCode(FEEDBACK_START_CHECK).isEmpty());
        assertTrue(SyncType.fromCode(FEEDBACK_CONSTRAINT).isEmpty());
        assertTrue(SyncType.fromCode(FEEDBACK_SHIFT_OUTPUT).isEmpty());
        assertTrue(SyncType.fromCode(FEEDBACK_MATERIAL_SHORTAGE).isEmpty());
        assertTrue(SyncType.fromCode(FEEDBACK_REQUISITION).isEmpty());
        assertTrue(SyncType.fromCode(FEEDBACK_SUPPLY_PROGRESS).isEmpty());
        assertTrue(SyncType.fromCode(FEEDBACK_STATUS_CHANGE).isEmpty());
        assertTrue(SyncType.fromCode(FEEDBACK_PROCESS_CHANGE).isEmpty());

        // 验证新增APS下发类型
        assertNotNull(SyncType.valueOf("MRP"));
        assertNotNull(SyncType.valueOf("RESOURCE_ALLOCATION"));
        assertNotNull(SyncType.valueOf("GANTT"));
        assertNotNull(SyncType.valueOf("CAPACITY_LOAD"));
        assertNotNull(SyncType.valueOf("SCHEDULE_CHANGE"));

        assertEquals(20, SyncType.values().length);

        System.out.println("✓ 4.1 SyncType合同枚举已收缩: 共20个类型（10原有+5主数据+5APS下发）");
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

    private void verifyUnsupportedFeedbackAudited(String syncType) {
        verify(upstreamSyncService, never()).enqueue(anyString(), anyString(),
                anyLong(), anyString(), anyInt(), anyString());
        verify(syncLogService).createLog(anyString(),
                eq(SyncDirection.UPSTREAM.getCode()), eq(syncType));
        verify(syncLogService).completeLog(any(), eq(0), eq(0), eq(1),
                contains("APS 当前合同不支持该执行反馈类型: " + syncType));
    }

    private ApsExtendedCallbackServiceImpl instantiateCallbackService() {
        try {
            Constructor<?> constructor = Arrays.stream(ApsExtendedCallbackServiceImpl.class.getDeclaredConstructors())
                    .max(Comparator.comparingInt(Constructor::getParameterCount))
                    .orElseThrow();
            constructor.setAccessible(true);

            Object[] args = Arrays.stream(constructor.getParameterTypes())
                    .map(this::resolveCallbackDependency)
                    .toArray();
            return (ApsExtendedCallbackServiceImpl) constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("初始化 APS 扩展回调服务失败", e);
        }
    }

    private Object resolveCallbackDependency(Class<?> parameterType) {
        return switch (parameterType.getName()) {
            case "com.mes.aps.service.IApsSyncLogService" -> syncLogService;
            case "com.fasterxml.jackson.databind.ObjectMapper" -> objectMapper;
            case "com.mes.workorder.mapper.WorkOrderMapper" -> workOrderMapper;
            case "com.mes.dispatch.mapper.DispatchTaskMapper" -> dispatchTaskMapper;
            case "com.mes.dispatch.mapper.DispatchAssignmentMapper" -> dispatchAssignmentMapper;
            case "com.mes.dispatch.service.IDispatchTaskService" -> dispatchTaskService;
            case "com.mes.aps.service.ApsCallbackIdempotencyService" -> idempotencyService;
            case "com.mes.aps.mapper.ApsGanttCacheMapper" -> ganttCacheMapper;
            case "com.mes.aps.mapper.ApsCapacityLoadMapper" -> capacityLoadMapper;
            case "com.mes.basic.mapper.MaterialMapper" -> materialMapper;
            case "com.mes.material.service.IMaterialRequisitionService" -> materialRequisitionService;
            default -> throw new IllegalStateException("未知的 APS 扩展回调依赖: " + parameterType.getName());
        };
    }

    private Object createOptionalRecorderProxy(String className, java.util.function.Consumer<RecordingInvocationHandler> consumer) {
        try {
            Class<?> mapperType = Class.forName(className);
            RecordingInvocationHandler handler = new RecordingInvocationHandler();
            consumer.accept(handler);
            return Proxy.newProxyInstance(
                    mapperType.getClassLoader(),
                    new Class<?>[]{mapperType},
                    handler);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Object readField(Object target, String fieldName) {
        assertNotNull(target, "目标对象不能为空");
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                java.lang.reflect.Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException("读取字段失败: " + fieldName, e);
            }
        }
        fail("未找到字段: " + fieldName);
        return null;
    }

    private static class RecordingInvocationHandler implements InvocationHandler {

        private final Map<String, List<Object[]>> invocations = new HashMap<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            invocations.computeIfAbsent(method.getName(), key -> new ArrayList<>())
                    .add(args == null ? new Object[0] : args.clone());

            Class<?> returnType = method.getReturnType();
            if (returnType == Integer.TYPE || returnType == Integer.class) {
                return 1;
            }
            if (returnType == Long.TYPE || returnType == Long.class) {
                return 1L;
            }
            if (returnType == Boolean.TYPE || returnType == Boolean.class) {
                return false;
            }
            return null;
        }

        int count(String methodName) {
            return invocations.getOrDefault(methodName, List.of()).size();
        }

        Object firstArgument(String methodName) {
            return argument(methodName, 0);
        }

        Object argument(String methodName, int index) {
            List<Object[]> calls = invocations.getOrDefault(methodName, List.of());
            assertTrue(calls.size() > index, "未捕获到第 " + (index + 1) + " 次 " + methodName + " 调用");
            Object[] args = calls.get(index);
            assertTrue(args.length > 0, methodName + " 调用缺少参数");
            return args[0];
        }
    }
}
