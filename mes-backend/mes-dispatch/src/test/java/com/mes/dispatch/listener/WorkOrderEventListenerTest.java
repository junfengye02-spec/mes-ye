package com.mes.dispatch.listener;

import com.mes.common.exception.BusinessException;
import com.mes.dispatch.service.IDispatchTaskService;
import com.mes.workorder.event.WorkOrderReleasedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkOrderEventListenerTest {

    @Mock
    private IDispatchTaskService dispatchTaskService;

    @InjectMocks
    private WorkOrderEventListener listener;

    @Test
    @DisplayName("工单下发后自动生成派工任务")
    void onWorkOrderReleased_generatesDispatchTasks() {
        listener.onWorkOrderReleased(new WorkOrderReleasedEvent(this, 10L));

        verify(dispatchTaskService).generateFromWorkOrder(10L);
    }

    @Test
    @DisplayName("派工生成失败时不应静默吞错")
    void onWorkOrderReleased_propagatesDispatchGenerationFailure() {
        doThrow(new BusinessException("工单没有工作清单，无法生成派工任务"))
                .when(dispatchTaskService).generateFromWorkOrder(10L);

        assertThrows(BusinessException.class,
                () -> listener.onWorkOrderReleased(new WorkOrderReleasedEvent(this, 10L)));
    }
}
