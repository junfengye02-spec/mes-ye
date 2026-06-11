package com.mes.aps.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.aps.domain.entity.ApsSyncLog;
import com.mes.aps.enums.SyncDirection;
import com.mes.aps.service.impl.ApsExecutionFeedbackServiceImpl;
import com.mes.dispatch.domain.entity.DispatchAssignment;
import com.mes.dispatch.domain.entity.DispatchTask;
import com.mes.dispatch.mapper.DispatchAssignmentMapper;
import com.mes.dispatch.mapper.DispatchTaskMapper;
import com.mes.material.mapper.MaterialRequisitionMapper;
import com.mes.quality.mapper.ShiftHandoverMapper;
import com.mes.workorder.mapper.WorkOrderConstraintMapper;
import com.mes.workorder.mapper.WorkOrderInputMaterialMapper;
import com.mes.workorder.mapper.WorkOrderMapper;
import com.mes.workorder.mapper.WorkOrderSupplyPlanMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApsExecutionFeedbackServiceTest {

    private static final String DISPATCH_FEEDBACK_TYPE = "DISPATCH";

    @Mock
    private IApsUpstreamSyncService upstreamSyncService;
    @Mock
    private IApsSyncLogService syncLogService;
    @Mock
    private DispatchTaskMapper dispatchTaskMapper;
    @Mock
    private DispatchAssignmentMapper dispatchAssignmentMapper;
    @Mock
    private ShiftHandoverMapper shiftHandoverMapper;
    @Mock
    private WorkOrderMapper workOrderMapper;
    @Mock
    private WorkOrderInputMaterialMapper inputMaterialMapper;
    @Mock
    private WorkOrderConstraintMapper constraintMapper;
    @Mock
    private WorkOrderSupplyPlanMapper supplyPlanMapper;
    @Mock
    private MaterialRequisitionMapper requisitionMapper;

    private ApsExecutionFeedbackServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApsExecutionFeedbackServiceImpl(
                upstreamSyncService,
                syncLogService,
                new ObjectMapper(),
                dispatchTaskMapper,
                dispatchAssignmentMapper,
                shiftHandoverMapper,
                workOrderMapper,
                inputMaterialMapper,
                constraintMapper,
                supplyPlanMapper,
                requisitionMapper
        );
    }

    @Test
    @DisplayName("执行反馈 - APS 不支持的反馈类型不入上行队列，改记本地失败审计")
    void feedbackDispatchAssignment_recordsUnsupportedAuditInsteadOfQueueing() {
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

        ApsSyncLog log = new ApsSyncLog();
        log.setId(99L);
        when(dispatchTaskMapper.selectById(100L)).thenReturn(task);
        when(dispatchAssignmentMapper.selectById(200L)).thenReturn(assignment);
        when(syncLogService.createLog(anyString(), eq(SyncDirection.UPSTREAM.getCode()), eq(DISPATCH_FEEDBACK_TYPE)))
                .thenReturn(log);

        service.feedbackDispatchAssignment(100L, 200L);

        verify(upstreamSyncService, never()).enqueue(anyString(), anyString(), anyLong(), anyString(), anyInt(), anyString());
        verify(syncLogService).createLog(anyString(), eq(SyncDirection.UPSTREAM.getCode()), eq(DISPATCH_FEEDBACK_TYPE));
        verify(syncLogService).completeLog(eq(99L), eq(0), eq(0), eq(1), contains("APS 当前合同不支持该执行反馈类型"));
    }
}
