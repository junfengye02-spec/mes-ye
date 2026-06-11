package com.mes.quality.service;

import com.mes.common.event.RecheckCompletedEvent;
import com.mes.common.exception.BusinessException;
import com.mes.quality.domain.dto.RecheckApproveDTO;
import com.mes.quality.domain.dto.RecheckReviewDTO;
import com.mes.quality.domain.entity.RecheckRequest;
import com.mes.quality.enums.RecheckStatus;
import com.mes.quality.mapper.RecheckOrderPlanMapper;
import com.mes.quality.mapper.RecheckRequestMapper;
import com.mes.quality.mapper.RecheckSerialMapper;
import com.mes.quality.service.impl.RecheckRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RecheckRequestServiceImpl")
class RecheckRequestServiceTest {

    @Mock
    private RecheckRequestMapper recheckRequestMapper;

    @Mock
    private RecheckOrderPlanMapper orderPlanMapper;

    @Mock
    private RecheckSerialMapper serialMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RecheckRequestServiceImpl recheckRequestService;

    @BeforeEach
    void setUp() {
        recheckRequestService = new RecheckRequestServiceImpl(orderPlanMapper, serialMapper, eventPublisher);
        ReflectionTestUtils.setField(recheckRequestService, "baseMapper", recheckRequestMapper);
    }

    @Test
    @DisplayName("submit - CREATED -> SUBMITTED")
    void submit_transitionsCreatedToSubmitted() {
        RecheckRequest entity = recheck(RecheckStatus.CREATED);
        when(recheckRequestMapper.selectById(1L)).thenReturn(entity);
        when(recheckRequestMapper.updateById(any(RecheckRequest.class))).thenReturn(1);

        recheckRequestService.submit(1L);

        verify(recheckRequestMapper).updateById(argThat(request ->
                RecheckStatus.SUBMITTED.getCode().equals(request.getStatus())));
    }

    @Test
    @DisplayName("review - SUBMITTED -> IN_REVIEW 并写入审核信息")
    void review_transitionsSubmittedToInReview() {
        RecheckRequest entity = recheck(RecheckStatus.SUBMITTED);
        when(recheckRequestMapper.selectById(1L)).thenReturn(entity);
        when(recheckRequestMapper.updateById(any(RecheckRequest.class))).thenReturn(1);

        RecheckReviewDTO dto = new RecheckReviewDTO();
        dto.setReviewer("qa.lead");
        dto.setReviewDate(LocalDate.of(2026, 5, 27));
        dto.setIsReasonable(1);

        recheckRequestService.review(1L, dto);

        ArgumentCaptor<RecheckRequest> captor = ArgumentCaptor.forClass(RecheckRequest.class);
        verify(recheckRequestMapper).updateById(captor.capture());
        assertEquals(RecheckStatus.IN_REVIEW.getCode(), captor.getValue().getStatus());
        assertEquals("qa.lead", captor.getValue().getReviewer());
        assertEquals(LocalDate.of(2026, 5, 27), captor.getValue().getReviewDate());
        assertEquals(1, captor.getValue().getIsReasonable());
    }

    @Test
    @DisplayName("approve - IN_REVIEW 可批准")
    void approve_transitionsInReviewToApproved() {
        RecheckRequest approved = recheck(RecheckStatus.IN_REVIEW);
        when(recheckRequestMapper.selectById(1L)).thenReturn(approved);
        when(recheckRequestMapper.updateById(any(RecheckRequest.class))).thenReturn(1);

        RecheckApproveDTO approveDTO = new RecheckApproveDTO();
        approveDTO.setApproved(true);
        recheckRequestService.approve(1L, approveDTO);

        verify(recheckRequestMapper).updateById(argThat(request ->
                RecheckStatus.APPROVED.getCode().equals(request.getStatus())));
    }

    @Test
    @DisplayName("approve - IN_REVIEW 可驳回")
    void approve_transitionsInReviewToRejected() {
        RecheckRequest rejected = recheck(RecheckStatus.IN_REVIEW);
        when(recheckRequestMapper.selectById(2L)).thenReturn(rejected);
        when(recheckRequestMapper.updateById(any(RecheckRequest.class))).thenReturn(1);

        RecheckApproveDTO approveDTO = new RecheckApproveDTO();
        approveDTO.setApproved(false);
        recheckRequestService.approve(2L, approveDTO);

        verify(recheckRequestMapper).updateById(argThat(request ->
                RecheckStatus.REJECTED.getCode().equals(request.getStatus())));
    }

    @Test
    @DisplayName("complete - 仅 APPROVED 可完结")
    void complete_onlyApprovedCanComplete() {
        RecheckRequest entity = recheck(RecheckStatus.APPROVED);
        entity.setProductionOrderNo("WO-001");
        entity.setMaterialName("工序一");
        when(recheckRequestMapper.selectById(1L)).thenReturn(entity);
        when(recheckRequestMapper.updateById(any(RecheckRequest.class))).thenReturn(1);

        recheckRequestService.complete(1L);

        verify(recheckRequestMapper).updateById(argThat(request ->
                RecheckStatus.COMPLETED.getCode().equals(request.getStatus())));
        verify(eventPublisher).publishEvent(argThat(RecheckCompletedEvent.class::isInstance));
    }

    @Test
    @DisplayName("submit - 非 CREATED 状态拒绝")
    void submit_rejectsInvalidSourceStatus() {
        RecheckRequest entity = recheck(RecheckStatus.SUBMITTED);
        when(recheckRequestMapper.selectById(1L)).thenReturn(entity);

        assertThrows(BusinessException.class, () -> recheckRequestService.submit(1L));
    }

    private static RecheckRequest recheck(RecheckStatus status) {
        RecheckRequest entity = new RecheckRequest();
        entity.setId(1L);
        entity.setMaterialCode("MAT-1");
        entity.setStatus(status.getCode());
        return entity;
    }
}
