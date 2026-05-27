package com.mes.workorder.listener;

import com.mes.common.event.DispatchAllTasksCompletedEvent;
import com.mes.common.event.DispatchTaskStartedEvent;
import com.mes.workorder.domain.entity.WorkOrder;
import com.mes.workorder.enums.WorkOrderStatus;
import com.mes.workorder.mapper.WorkOrderMapper;
import com.mes.workorder.service.IWorkOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchLifecycleEventListenerTest {

    @Mock
    private WorkOrderMapper workOrderMapper;
    @Mock
    private IWorkOrderService workOrderService;

    @InjectMocks
    private DispatchLifecycleEventListener listener;

    @Test
    @DisplayName("派工开工事件触发工单自动开工")
    void onDispatchTaskStarted_startsReleasedWorkOrder() {
        when(workOrderMapper.selectById(10L)).thenReturn(workOrder(10L, WorkOrderStatus.RELEASED));

        listener.onDispatchTaskStarted(new DispatchTaskStartedEvent(
                this, 1L, 10L, 101L, "WO-001", "OP10", "工序一"));

        verify(workOrderService).start(10L);
    }

    @Test
    @DisplayName("派工全部完工事件触发工单完工")
    void onDispatchAllTasksCompleted_completesWorkOrder() {
        when(workOrderMapper.selectById(10L)).thenReturn(workOrder(10L, WorkOrderStatus.IN_PROGRESS));

        listener.onDispatchAllTasksCompleted(new DispatchAllTasksCompletedEvent(this, 10L, "WO-001"));

        verify(workOrderService, never()).start(10L);
        verify(workOrderService).complete(10L);
    }

    private static WorkOrder workOrder(Long id, WorkOrderStatus status) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(id);
        workOrder.setStatus(status.getCode());
        return workOrder;
    }
}
