package com.mes.dispatch.listener;

import com.mes.common.event.AbnormalSubmittedEvent;
import com.mes.dispatch.domain.entity.DispatchTask;
import com.mes.dispatch.enums.DispatchStatus;
import com.mes.dispatch.mapper.DispatchTaskMapper;
import com.mes.dispatch.service.IDispatchStatusLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbnormalEventListenerTest {

    @Mock
    private DispatchTaskMapper dispatchTaskMapper;
    @Mock
    private IDispatchStatusLogService statusLogService;

    @InjectMocks
    private AbnormalEventListener listener;

    @Test
    @DisplayName("异常提交后将关联派工任务标记为异常")
    void onAbnormalSubmitted_marksDispatchTaskAbnormal() {
        DispatchTask task = new DispatchTask();
        task.setId(1L);
        task.setDispatchStatus(DispatchStatus.IN_PROGRESS.getCode());
        when(dispatchTaskMapper.selectById(1L)).thenReturn(task);
        when(dispatchTaskMapper.updateById(any(DispatchTask.class))).thenReturn(1);

        listener.onAbnormalSubmitted(new AbnormalSubmittedEvent(
                this, 99L, "YC-001", 10L, 1L, "WO-001", "PROCESS_ABNORMAL", "发现异常"));

        verify(dispatchTaskMapper).updateById(any(DispatchTask.class));
        verify(statusLogService).log(eq(1L), eq(DispatchStatus.IN_PROGRESS.getCode()),
                eq(DispatchStatus.ABNORMAL.getCode()), eq("异常提报"), contains("YC-001"));
    }
}
