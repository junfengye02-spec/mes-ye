package com.mes.quality.listener;

import com.mes.common.event.AbnormalSubmittedEvent;
import com.mes.common.event.DispatchTaskQualityFailedEvent;
import com.mes.common.event.DispatchTaskStartedEvent;
import com.mes.quality.domain.dto.RecheckRequestDTO;
import com.mes.quality.domain.dto.WorkStartCheckDTO;
import com.mes.quality.service.IRecheckRequestService;
import com.mes.quality.service.IWorkStartCheckService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QualityEventListenerTest {

    @Mock
    private IRecheckRequestService recheckRequestService;
    @Mock
    private IWorkStartCheckService workStartCheckService;

    @InjectMocks
    private QualityEventListener listener;

    @Test
    @DisplayName("派工开工事件自动创建开工检查")
    void onDispatchTaskStarted_createsWorkStartCheck() {
        listener.onDispatchTaskStarted(new DispatchTaskStartedEvent(
                this, 1L, 10L, 101L, "WO-001", "OP10", "工序一"));

        ArgumentCaptor<WorkStartCheckDTO> captor = ArgumentCaptor.forClass(WorkStartCheckDTO.class);
        verify(workStartCheckService).create(captor.capture());
        WorkStartCheckDTO dto = captor.getValue();
        assertEquals(10L, dto.getWorkOrderId());
        assertEquals(101L, dto.getWorkOrderTaskId());
        assertEquals("WO-001", dto.getWorkOrderNo());
        assertEquals("OP10", dto.getWorkNo());
        assertEquals("PENDING", dto.getCheckStatus());
    }

    @Test
    @DisplayName("派工质量 FAIL 事件自动创建复检申请")
    void onDispatchTaskQualityFailed_createsRecheckRequest() {
        listener.onDispatchTaskQualityFailed(new DispatchTaskQualityFailedEvent(
                this, 1L, 10L, 101L, "WO-001", "OP10", "工序一",
                "项目A", "SN-001", new BigDecimal("5"),
                LocalDateTime.of(2026, 5, 27, 12, 0), "尺寸超差", "system"));

        ArgumentCaptor<RecheckRequestDTO> captor = ArgumentCaptor.forClass(RecheckRequestDTO.class);
        verify(recheckRequestService).create(captor.capture());
        RecheckRequestDTO dto = captor.getValue();
        assertEquals(10L, dto.getWorkOrderId());
        assertEquals(1L, dto.getDispatchTaskId());
        assertEquals("WO-001", dto.getProductionOrderNo());
        assertTrue(dto.getRecheckReason().contains("FAIL"));
    }

    @Test
    @DisplayName("异常联络单提交事件触发质量复检")
    void onAbnormalSubmitted_createsRecheckRequest() {
        listener.onAbnormalSubmitted(new AbnormalSubmittedEvent(
                this, 99L, "YC-001", 10L, 1L, "ORD-001", "WO-001",
                "PROCESS_ABNORMAL", "发现异常"));

        ArgumentCaptor<RecheckRequestDTO> captor = ArgumentCaptor.forClass(RecheckRequestDTO.class);
        verify(recheckRequestService).create(captor.capture());
        RecheckRequestDTO dto = captor.getValue();
        assertEquals(10L, dto.getWorkOrderId());
        assertEquals(1L, dto.getDispatchTaskId());
        assertEquals("WO-001", dto.getProductionOrderNo());
        assertTrue(dto.getRecheckReason().contains("YC-001"));
    }

    @Test
    @DisplayName("异常联络单仅提供订单号时仍兼容回填复检生产单号")
    void onAbnormalSubmitted_fallsBackToOrderNoWhenWorkOrderNoMissing() {
        listener.onAbnormalSubmitted(new AbnormalSubmittedEvent(
                this, 100L, "YC-002", 10L, 1L, "ORD-002", null,
                "PROCESS_ABNORMAL", "发现异常"));

        ArgumentCaptor<RecheckRequestDTO> captor = ArgumentCaptor.forClass(RecheckRequestDTO.class);
        verify(recheckRequestService, org.mockito.Mockito.times(1)).create(captor.capture());
        RecheckRequestDTO dto = captor.getValue();
        assertEquals("ORD-002", dto.getProductionOrderNo());
    }
}
