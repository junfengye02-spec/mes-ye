package com.mes.query.service;

import com.mes.common.event.DispatchTaskCompletedEvent;
import com.mes.common.event.RecheckCompletedEvent;
import com.mes.query.domain.entity.InspectionWork;
import com.mes.query.mapper.InspectionWorkMapper;
import com.mes.query.service.impl.InspectionWorkServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InspectionWorkServiceImpl")
class InspectionWorkServiceTest {

    @Mock
    private InspectionWorkMapper inspectionWorkMapper;

    private InspectionWorkServiceImpl inspectionWorkService;

    @BeforeEach
    void setUp() {
        inspectionWorkService = new InspectionWorkServiceImpl();
        ReflectionTestUtils.setField(inspectionWorkService, "baseMapper", inspectionWorkMapper);
    }

    @Test
    @DisplayName("projectDispatchCompletion - 派工完工事件生成检验作业记录")
    void projectDispatchCompletion_createsInspectionWorkProjection() {
        when(inspectionWorkMapper.selectOne(any())).thenReturn(null);
        when(inspectionWorkMapper.insert(any(InspectionWork.class))).thenReturn(1);

        inspectionWorkService.projectDispatchCompletion(new DispatchTaskCompletedEvent(
                this,
                1L,
                10L,
                101L,
                "WO-001",
                "OP10",
                "工序一",
                "项目A",
                "SN-001",
                new BigDecimal("5"),
                "FAIL",
                LocalDateTime.of(2026, 5, 27, 12, 0),
                "尺寸超差",
                "system"
        ));

        ArgumentCaptor<InspectionWork> captor = ArgumentCaptor.forClass(InspectionWork.class);
        verify(inspectionWorkMapper).insert(captor.capture());
        InspectionWork projection = captor.getValue();
        assertEquals("IW-DISPATCH-1", projection.getWorkNo());
        assertEquals("工序一完工检验", projection.getWorkName());
        assertEquals(new BigDecimal("5"), projection.getPlanInspectQty());
        assertEquals(new BigDecimal("5"), projection.getInspectedQty());
        assertEquals(BigDecimal.ZERO, projection.getQualifiedQty());
        assertEquals(new BigDecimal("5"), projection.getUnqualifiedQty());
        assertEquals("不合格", projection.getJudgment());
        assertEquals("COMPLETED", projection.getWorkStatus());
        assertEquals("完工检验", projection.getInspectCategory());
        assertEquals("过程检验", projection.getInspectType());
        assertEquals(10L, projection.getWorkOrderId());
        assertEquals("WO-001", projection.getWorkOrderNo());
        assertTrue(projection.getDescription().contains("尺寸超差"));
    }

    @Test
    @DisplayName("projectRecheckCompletion - 复检完结事件生成复检作业记录")
    void projectRecheckCompletion_createsInspectionWorkProjection() {
        when(inspectionWorkMapper.selectOne(any())).thenReturn(null);
        when(inspectionWorkMapper.insert(any(InspectionWork.class))).thenReturn(1);

        inspectionWorkService.projectRecheckCompletion(new RecheckCompletedEvent(
                this,
                9L,
                10L,
                1L,
                "WO-001",
                "MAT-001",
                "工序一",
                LocalDateTime.of(2026, 5, 27, 14, 0)
        ));

        ArgumentCaptor<InspectionWork> captor = ArgumentCaptor.forClass(InspectionWork.class);
        verify(inspectionWorkMapper).insert(captor.capture());
        InspectionWork projection = captor.getValue();
        assertEquals("IW-RECHECK-9", projection.getWorkNo());
        assertEquals("工序一复检", projection.getWorkName());
        assertEquals("COMPLETED", projection.getWorkStatus());
        assertEquals("复检", projection.getInspectCategory());
        assertEquals("复检", projection.getInspectType());
        assertEquals(10L, projection.getWorkOrderId());
        assertEquals("WO-001", projection.getWorkOrderNo());
        assertTrue(projection.getDescription().contains("复检申请 9"));
    }
}
