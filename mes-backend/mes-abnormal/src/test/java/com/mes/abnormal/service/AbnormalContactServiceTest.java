package com.mes.abnormal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.abnormal.domain.dto.AbnormalContactAttachmentDTO;
import com.mes.abnormal.domain.dto.AbnormalContactDTO;
import com.mes.abnormal.domain.entity.AbnormalContact;
import com.mes.abnormal.domain.entity.AbnormalContactAttachment;
import com.mes.abnormal.domain.vo.AbnormalContactAttachmentVO;
import com.mes.abnormal.enums.AbnormalContactStatus;
import com.mes.common.event.AbnormalSubmittedEvent;
import com.mes.abnormal.mapper.AbnormalContactAttachmentMapper;
import com.mes.abnormal.mapper.AbnormalContactLogMapper;
import com.mes.abnormal.mapper.AbnormalContactMapper;
import com.mes.abnormal.service.impl.AbnormalContactServiceImpl;
import com.mes.common.event.ApsSyncEvent;
import com.mes.common.exception.BusinessException;
import com.mes.workorder.domain.entity.WorkOrder;
import com.mes.workorder.mapper.WorkOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link com.mes.abnormal.service.impl.AbnormalContactServiceImpl} 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AbnormalContactServiceTest {

    @Mock
    private AbnormalContactMapper contactMapper;
    @Mock
    private AbnormalContactAttachmentMapper attachmentMapper;
    @Mock
    private AbnormalContactLogMapper logMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private WorkOrderMapper workOrderMapper;

    private AbnormalContactServiceImpl contactService;

    @BeforeEach
    void injectBaseMapper() {
        contactService = spy(new AbnormalContactServiceImpl(attachmentMapper, logMapper, eventPublisher, workOrderMapper));
        ReflectionTestUtils.setField(contactService, "baseMapper", contactMapper);
        lenient().doReturn(true).when(contactService).removeById(any(Serializable.class));
    }

    @Test
    @DisplayName("1. 创建联络单 - 自动生成单号")
    void create_autoContactNo() {
        AbnormalContactDTO dto = dto(null, "主题A");
        when(contactMapper.insert(any(AbnormalContact.class))).thenAnswer(inv -> {
            AbnormalContact e = inv.getArgument(0);
            e.setId(100L);
            return 1;
        });
        when(logMapper.insert(any())).thenReturn(1);

        Long id = contactService.create(dto);

        assertNotNull(id);
        verify(contactMapper).insert(argThat(c -> {
            assertNotNull(c.getContactNo());
            assertTrue(c.getContactNo().startsWith("YC-"));
            assertEquals(AbnormalContactStatus.DRAFT.getCode(), c.getStatus());
            return true;
        }));
    }

    @Test
    @DisplayName("1.1 创建联络单 - 带工单ID时自动回填标准订单号")
    void create_normalizesOrderNoFromWorkOrder() {
        AbnormalContactDTO dto = dto(null, "主题A");
        dto.setWorkOrderId(10L);
        when(workOrderMapper.selectById(10L)).thenReturn(workOrderWithOrderNo(10L, "WO-001", "ORD-001"));
        when(contactMapper.insert(any(AbnormalContact.class))).thenAnswer(inv -> {
            AbnormalContact e = inv.getArgument(0);
            e.setId(101L);
            return 1;
        });
        when(logMapper.insert(any())).thenReturn(1);

        contactService.create(dto);

        verify(contactMapper).insert(argThat(c ->
                Long.valueOf(10L).equals(c.getWorkOrderId())
                        && "ORD-001".equals(c.getOrderNo())));
    }

    @Test
    @DisplayName("1.2 创建联络单 - 工单关联的订单号与手输值不一致时拒绝")
    void create_rejectsMismatchedOrderNoAgainstWorkOrder() {
        AbnormalContactDTO dto = dto(null, "主题A");
        dto.setWorkOrderId(10L);
        dto.setOrderNo("ORD-WRONG");
        when(workOrderMapper.selectById(10L)).thenReturn(workOrderWithOrderNo(10L, "WO-001", "ORD-001"));

        BusinessException ex = assertThrows(BusinessException.class, () -> contactService.create(dto));
        assertTrue(ex.getMessage().contains("订单号"));
        verify(contactMapper, never()).insert(any());
    }

    @Test
    @DisplayName("2. 创建联络单 - 手工单号重复")
    void create_duplicateManualNo() {
        AbnormalContactDTO dto = dto("YC-DUP-001", "主题");
        when(contactMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> contactService.create(dto));
        assertTrue(ex.getMessage().contains("异常联络单号已存在"));
    }

    @Test
    @DisplayName("3. 更新联络单 - 仅 DRAFT 可编辑")
    void update_onlyDraftAllowed() {
        AbnormalContact submitted = contact(1L, "YC-01", AbnormalContactStatus.SUBMITTED);
        when(contactMapper.selectById(1L)).thenReturn(submitted);

        assertThrows(BusinessException.class, () -> contactService.update(1L, dto("YC-01", "改主题")));
    }

    @Test
    @DisplayName("4. 更新联络单 - 单号不可修改")
    void update_contactNoImmutable() {
        AbnormalContact draft = contact(1L, "YC-KEEP", AbnormalContactStatus.DRAFT);
        AbnormalContactDTO dto = dto("YC-NEW-NO", "新主题");
        when(contactMapper.selectById(1L)).thenReturn(draft);
        when(contactMapper.updateById(any(AbnormalContact.class))).thenReturn(1);

        contactService.update(1L, dto);

        verify(contactMapper).updateById(argThat(c ->
                "YC-KEEP".equals(c.getContactNo())
                        && "新主题".equals(c.getSubject())
                        && AbnormalContactStatus.DRAFT.getCode().equals(c.getStatus())));
    }

    @Test
    @DisplayName("4.1 更新联络单 - 带工单ID时同步规范化订单号")
    void update_normalizesOrderNoFromWorkOrder() {
        AbnormalContact draft = contact(1L, "YC-KEEP", AbnormalContactStatus.DRAFT);
        AbnormalContactDTO dto = dto("YC-KEEP", "新主题");
        dto.setWorkOrderId(10L);
        when(contactMapper.selectById(1L)).thenReturn(draft);
        when(workOrderMapper.selectById(10L)).thenReturn(workOrderWithOrderNo(10L, "WO-001", "ORD-001"));
        when(contactMapper.updateById(any(AbnormalContact.class))).thenReturn(1);

        contactService.update(1L, dto);

        verify(contactMapper).updateById(argThat(c ->
                Long.valueOf(10L).equals(c.getWorkOrderId())
                        && "ORD-001".equals(c.getOrderNo())));
    }

    @Test
    @DisplayName("5. 删除联络单 - 仅 DRAFT 可删除")
    void delete_onlyDraft() {
        AbnormalContact draft = contact(1L, "YC-01", AbnormalContactStatus.DRAFT);
        when(contactMapper.selectById(1L)).thenReturn(draft);
        when(attachmentMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(logMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        contactService.delete(1L);

        verify(attachmentMapper).delete(any());
        verify(logMapper).delete(any());
        verify(contactService).removeById(1L);

        AbnormalContact processing = contact(2L, "YC-02", AbnormalContactStatus.PROCESSING);
        when(contactMapper.selectById(2L)).thenReturn(processing);
        assertThrows(BusinessException.class, () -> contactService.delete(2L));
    }

    @Test
    @DisplayName("6. 提交 - DRAFT→SUBMITTED（设置 publishTime）")
    void submit_draftToSubmitted_setsPublishTime() {
        AbnormalContact draft = contact(1L, "YC-01", AbnormalContactStatus.DRAFT);
        draft.setAffectSchedule(0);
        when(contactMapper.selectById(1L)).thenReturn(draft);
        when(contactMapper.updateById(any(AbnormalContact.class))).thenReturn(1);
        when(logMapper.insert(any())).thenReturn(1);

        contactService.submit(1L);

        verify(contactMapper).updateById(argThat(c ->
                AbnormalContactStatus.SUBMITTED.getCode().equals(c.getStatus())
                        && c.getPublishTime() != null));
    }

    @Test
    @DisplayName("7. 提交时影响排程 - 触发 APS 重排事件")
    void submit_affectSchedule_publishesApsEvent() {
        AbnormalContact draft = contact(1L, "YC-APS", AbnormalContactStatus.DRAFT);
        draft.setAffectSchedule(1);
        draft.setEventCategory("LINE_DOWN");
        when(contactMapper.selectById(1L)).thenReturn(draft);
        when(contactMapper.updateById(any(AbnormalContact.class))).thenReturn(1);
        when(logMapper.insert(any())).thenReturn(1);

        contactService.submit(1L);

        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());
        ApsSyncEvent ev = captor.getAllValues().stream()
                .filter(ApsSyncEvent.class::isInstance)
                .map(ApsSyncEvent.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals("ABNORMAL", ev.getSyncType());
        assertEquals("LINE_DOWN", ev.getDataType());
    }

    @Test
    @DisplayName("8. 提交时 APS 防抖（5分钟内同分类不重复触发）")
    void submit_apsDebounce_sameCategoryWithinFiveMinutes() {
        AbnormalContact c1 = contact(1L, "YC-A1", AbnormalContactStatus.DRAFT);
        c1.setAffectSchedule(1);
        c1.setEventCategory("SAME_CAT");
        AbnormalContact c2 = contact(2L, "YC-A2", AbnormalContactStatus.DRAFT);
        c2.setAffectSchedule(1);
        c2.setEventCategory("SAME_CAT");

        when(contactMapper.selectById(1L)).thenReturn(c1);
        when(contactMapper.selectById(2L)).thenReturn(c2);
        when(contactMapper.updateById(any(AbnormalContact.class))).thenReturn(1);
        when(logMapper.insert(any())).thenReturn(1);

        contactService.submit(1L);
        contactService.submit(2L);

        long apsEvents = mockingDetails(eventPublisher).getInvocations().stream()
                .map(invocation -> invocation.getArgument(0))
                .filter(ApsSyncEvent.class::isInstance)
                .count();
        assertEquals(1L, apsEvents);
    }

    @Test
    @DisplayName("8.1 提交时发布 AbnormalSubmittedEvent")
    void submit_publishesAbnormalSubmittedEvent() {
        AbnormalContact draft = contact(1L, "YC-LINK", AbnormalContactStatus.DRAFT);
        draft.setAffectSchedule(0);
        draft.setWorkOrderId(10L);
        draft.setDispatchTaskId(20L);
        draft.setOrderNo("ORD-001");
        when(workOrderMapper.selectById(10L)).thenReturn(workOrder(10L, "WO-001"));
        when(contactMapper.selectById(1L)).thenReturn(draft);
        when(contactMapper.updateById(any(AbnormalContact.class))).thenReturn(1);
        when(logMapper.insert(any())).thenReturn(1);

        contactService.submit(1L);

        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertTrue(captor.getValue() instanceof AbnormalSubmittedEvent);
        AbnormalSubmittedEvent event = (AbnormalSubmittedEvent) captor.getValue();
        assertEquals(10L, event.getWorkOrderId());
        assertEquals(20L, event.getDispatchTaskId());
        assertEquals("ORD-001", event.getOrderNo());
        assertEquals("WO-001", event.getWorkOrderNo());
    }

    @Test
    @DisplayName("9. 处理 - SUBMITTED→PROCESSING")
    void process_submittedToProcessing() {
        AbnormalContact submitted = contact(1L, "YC-01", AbnormalContactStatus.SUBMITTED);
        when(contactMapper.selectById(1L)).thenReturn(submitted);
        when(contactMapper.updateById(any(AbnormalContact.class))).thenReturn(1);
        when(logMapper.insert(any())).thenReturn(1);

        contactService.process(1L);

        verify(contactMapper).updateById(argThat(c ->
                AbnormalContactStatus.PROCESSING.getCode().equals(c.getStatus())));
    }

    @Test
    @DisplayName("10. 关闭 - PROCESSING→CLOSED")
    void close_processingToClosed() {
        AbnormalContact processing = contact(1L, "YC-01", AbnormalContactStatus.PROCESSING);
        when(contactMapper.selectById(1L)).thenReturn(processing);
        when(contactMapper.updateById(any(AbnormalContact.class))).thenReturn(1);
        when(logMapper.insert(any())).thenReturn(1);

        contactService.close(1L);

        verify(contactMapper).updateById(argThat(c ->
                AbnormalContactStatus.CLOSED.getCode().equals(c.getStatus())));
    }

    @Test
    @DisplayName("11. 非正确前置状态不允许状态流转")
    void invalidStateTransitions_rejected() {
        when(contactMapper.selectById(1L)).thenReturn(contact(1L, "YC-1", AbnormalContactStatus.SUBMITTED));
        assertThrows(BusinessException.class, () -> contactService.submit(1L));

        when(contactMapper.selectById(2L)).thenReturn(contact(2L, "YC-2", AbnormalContactStatus.DRAFT));
        assertThrows(BusinessException.class, () -> contactService.process(2L));

        when(contactMapper.selectById(3L)).thenReturn(contact(3L, "YC-3", AbnormalContactStatus.SUBMITTED));
        assertThrows(BusinessException.class, () -> contactService.close(3L));

        when(contactMapper.selectById(4L)).thenReturn(contact(4L, "YC-4", AbnormalContactStatus.CLOSED));
        assertThrows(BusinessException.class, () -> contactService.process(4L));

        verify(contactMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("12. 附件添加、删除、签署")
    void attachments_addDeleteSign() {
        AbnormalContact draft = contact(1L, "YC-01", AbnormalContactStatus.DRAFT);
        when(contactMapper.selectById(1L)).thenReturn(draft);
        when(attachmentMapper.insert(any(AbnormalContactAttachment.class))).thenAnswer(inv -> {
            AbnormalContactAttachment a = inv.getArgument(0);
            a.setId(700L);
            return 1;
        });

        AbnormalContactAttachmentDTO addDto = new AbnormalContactAttachmentDTO();
        addDto.setFileName("a.pdf");
        addDto.setFileUrl("/files/a.pdf");
        addDto.setSignatureProvider("FADADA");
        Long attId = contactService.addAttachment(1L, addDto);
        assertEquals(700L, attId);
        verify(attachmentMapper, atLeastOnce()).insert(argThat(a ->
                Integer.valueOf(0).equals(a.getSigned())
                        && "UNSIGNED".equals(a.getSignatureStatus())
                        && "FADADA".equals(a.getSignatureProvider())
                        && a.getFadadaFlag() == null));

        AbnormalContactAttachment unsigned = new AbnormalContactAttachment();
        unsigned.setId(700L);
        unsigned.setSigned(0);
        when(attachmentMapper.selectById(700L)).thenReturn(unsigned);
        when(attachmentMapper.deleteById(700L)).thenReturn(1);
        contactService.deleteAttachment(700L);
        verify(attachmentMapper).deleteById(700L);

        AbnormalContactAttachment toSign = new AbnormalContactAttachment();
        toSign.setId(701L);
        toSign.setSigned(0);
        toSign.setSignatureProvider("FADADA");
        when(attachmentMapper.selectById(701L)).thenReturn(toSign);
        when(attachmentMapper.updateById(any())).thenReturn(1);
        contactService.signAttachment(701L);
        verify(attachmentMapper).updateById(argThat(a ->
                a.getSigned() == 1
                        && "SIGNED".equals(a.getSignatureStatus())
                        && "FADADA".equals(a.getSignatureProvider())));
    }

    @Test
    @DisplayName("12.1 附件列表 - 返回通用签章字段")
    void listAttachments_exposesGenericSignatureFields() {
        AbnormalContactAttachment attachment = new AbnormalContactAttachment();
        attachment.setId(801L);
        attachment.setContactId(1L);
        attachment.setFileName("signed.pdf");
        attachment.setSignatureProvider("FADADA");
        attachment.setSignatureStatus("SIGNED");
        when(attachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(attachment));

        AbnormalContactAttachmentVO vo = contactService.listAttachments(1L).get(0);

        assertEquals("FADADA", vo.getSignatureProvider());
        assertEquals("SIGNED", vo.getSignatureStatus());
    }

    @Test
    @DisplayName("12.2 附件返回模型 - 不再暴露 vendor 专属字段")
    void attachmentVo_doesNotExposeVendorSpecificFlag() {
        assertTrue(Arrays.stream(AbnormalContactAttachmentVO.class.getDeclaredFields())
                .noneMatch(field -> "fadadaFlag".equals(field.getName())));
    }

    @Test
    @DisplayName("12.3 附件列表 - 兼容旧字段回填通用签章信息")
    void listAttachments_backfillsGenericSignatureFieldsFromLegacyVendorFlag() {
        AbnormalContactAttachment attachment = new AbnormalContactAttachment();
        attachment.setId(802L);
        attachment.setContactId(1L);
        attachment.setFileName("legacy-signed.pdf");
        attachment.setFadadaFlag("FADADA");
        attachment.setSigned(1);
        when(attachmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(attachment));

        AbnormalContactAttachmentVO vo = contactService.listAttachments(1L).get(0);

        assertEquals("FADADA", vo.getSignatureProvider());
        assertEquals("SIGNED", vo.getSignatureStatus());
    }

    @Test
    @DisplayName("13. 已签署附件不可删除")
    void deleteAttachment_signedNotAllowed() {
        AbnormalContactAttachment signed = new AbnormalContactAttachment();
        signed.setId(1L);
        signed.setSigned(1);
        when(attachmentMapper.selectById(1L)).thenReturn(signed);

        assertThrows(BusinessException.class, () -> contactService.deleteAttachment(1L));
    }

    @Test
    @DisplayName("14. 不可重复签署")
    void signAttachment_twiceNotAllowed() {
        AbnormalContactAttachment signed = new AbnormalContactAttachment();
        signed.setId(1L);
        signed.setSigned(1);
        when(attachmentMapper.selectById(1L)).thenReturn(signed);

        assertThrows(BusinessException.class, () -> contactService.signAttachment(1L));
    }

    private static AbnormalContactDTO dto(String contactNo, String subject) {
        AbnormalContactDTO d = new AbnormalContactDTO();
        d.setContactNo(contactNo);
        d.setSubject(subject);
        return d;
    }

    private static AbnormalContact contact(Long id, String no, AbnormalContactStatus status) {
        AbnormalContact c = new AbnormalContact();
        c.setId(id);
        c.setContactNo(no);
        c.setStatus(status.getCode());
        c.setOrderNo("WO-001");
        return c;
    }

    private static WorkOrder workOrder(Long id, String workOrderNo) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(id);
        workOrder.setWorkOrderNo(workOrderNo);
        return workOrder;
    }

    private static WorkOrder workOrderWithOrderNo(Long id, String workOrderNo, String orderNo) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(id);
        workOrder.setWorkOrderNo(workOrderNo);
        workOrder.setOrderNo(orderNo);
        return workOrder;
    }
}
