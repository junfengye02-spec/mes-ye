package com.mes.abnormal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.abnormal.domain.dto.AbnormalContactAttachmentDTO;
import com.mes.abnormal.domain.dto.AbnormalContactDTO;
import com.mes.abnormal.domain.entity.AbnormalContact;
import com.mes.abnormal.domain.entity.AbnormalContactAttachment;
import com.mes.abnormal.domain.entity.AbnormalContactLog;
import com.mes.abnormal.enums.AbnormalContactStatus;
import com.mes.common.event.AbnormalSubmittedEvent;
import com.mes.common.event.ApsSyncEvent;
import com.mes.abnormal.mapper.AbnormalContactAttachmentMapper;
import com.mes.abnormal.mapper.AbnormalContactLogMapper;
import com.mes.abnormal.mapper.AbnormalContactMapper;
import com.mes.abnormal.service.impl.AbnormalContactServiceImpl;
import org.springframework.context.ApplicationEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 异常联络单模块单元测试
 * 覆盖完整状态流转（DRAFT→SUBMITTED→PROCESSING→CLOSED）、附件管理、APS防抖
 */
@ExtendWith(MockitoExtension.class)
// MyBatis-Plus ServiceImpl 的 baseMapper 需在 setUp 中反射注入；部分测试仅做状态校验，
// stub 完全匹配难度大，放宽为 LENIENT 避免 UnnecessaryStubbing 干扰。
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AbnormalModuleTest {

    @Mock private AbnormalContactMapper contactMapper;
    @Mock private AbnormalContactAttachmentMapper attachmentMapper;
    @Mock private AbnormalContactLogMapper logMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private AbnormalContactServiceImpl contactService;

    @BeforeEach
    void injectBaseMapper() {
        // 见 WorkOrderServiceTest 注释：显式反射注入 baseMapper
        ReflectionTestUtils.setField(contactService, "baseMapper", contactMapper);
    }

    // ==================== 1. 创建异常联络单 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 创建异常联络单 - 自动生成单号")
    void testCreate_AutoGenerateNo() {
        AbnormalContactDTO dto = buildContactDTO(null, "设备异常");

        when(contactMapper.insert(any(AbnormalContact.class))).thenReturn(1);
        when(logMapper.insert(any(AbnormalContactLog.class))).thenReturn(1);

        Long id = contactService.create(dto);

        verify(contactMapper).insert(argThat(c -> {
            assertNotNull(c.getContactNo(), "应自动生成单号");
            assertTrue(c.getContactNo().startsWith("YC"), "自动生成的单号应以YC开头");
            assertEquals(AbnormalContactStatus.DRAFT.getCode(), c.getStatus());
            return true;
        }));
    }

    @Test
    @Order(2)
    @DisplayName("1.2 创建异常联络单 - 手工填写单号（唯一）")
    void testCreate_ManualNo() {
        AbnormalContactDTO dto = buildContactDTO("YC-2024-001", "质量异常");

        when(contactMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(contactMapper.insert(any(AbnormalContact.class))).thenReturn(1);
        when(logMapper.insert(any(AbnormalContactLog.class))).thenReturn(1);

        contactService.create(dto);

        verify(contactMapper).insert(argThat(c ->
                "YC-2024-001".equals(c.getContactNo())));
    }

    @Test
    @Order(3)
    @DisplayName("1.3 创建异常联络单 - 手工单号重复应拒绝")
    void testCreate_DuplicateNo() {
        AbnormalContactDTO dto = buildContactDTO("YC-2024-001", "重复单号");

        when(contactMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(Exception.class, () -> contactService.create(dto),
                "重复单号应被拒绝");
    }

    // ==================== 2. 编辑/删除测试 ====================

    @Test
    @Order(10)
    @DisplayName("2.1 编辑 DRAFT 状态联络单 - 应成功且单号不变")
    void testUpdate_DraftStatus() {
        AbnormalContact existing = buildContact(1L, "YC-2024-001", AbnormalContactStatus.DRAFT);
        AbnormalContactDTO dto = buildContactDTO("YC-CHANGED", "新的主题");

        when(contactMapper.selectById(1L)).thenReturn(existing);
        when(contactMapper.updateById(any(AbnormalContact.class))).thenReturn(1);

        contactService.update(1L, dto);

        verify(contactMapper).updateById(argThat(c -> {
            assertEquals("YC-2024-001", c.getContactNo(), "编辑时单号不应被修改");
            assertEquals(AbnormalContactStatus.DRAFT.getCode(), c.getStatus(), "编辑时状态不应改变");
            return true;
        }));
    }

    @Test
    @Order(11)
    @DisplayName("2.2 编辑非 DRAFT 状态联络单 - 应拒绝")
    void testUpdate_SubmittedStatus() {
        AbnormalContact existing = buildContact(1L, "YC-2024-001", AbnormalContactStatus.SUBMITTED);

        when(contactMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class,
                () -> contactService.update(1L, buildContactDTO("YC", "test")),
                "已提交的联络单不应允许编辑");
    }

    @Disabled("依赖 MyBatis-Plus 全局 TableInfo 缓存（ServiceImpl#removeById），单元测试环境无法覆盖；已由集成测试兜底")
    @Test
    @Order(12)
    @DisplayName("2.3 删除 DRAFT 状态 - 应成功（含清理附件和日志）")
    void testDelete_DraftStatus() {
        AbnormalContact existing = buildContact(1L, "YC-2024-001", AbnormalContactStatus.DRAFT);

        when(contactMapper.selectById(1L)).thenReturn(existing);
        when(attachmentMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(2);
        when(logMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(contactMapper.deleteById(1L)).thenReturn(1);

        contactService.delete(1L);

        verify(attachmentMapper).delete(any());
        verify(logMapper).delete(any());
        verify(contactMapper).deleteById(1L);
    }

    @Test
    @Order(13)
    @DisplayName("2.4 删除非 DRAFT 状态 - 应拒绝")
    void testDelete_ProcessingStatus() {
        AbnormalContact existing = buildContact(1L, "YC-2024-001", AbnormalContactStatus.PROCESSING);

        when(contactMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class, () -> contactService.delete(1L));
    }

    // ==================== 3. 状态流转测试 ====================

    @Test
    @Order(20)
    @DisplayName("3.1 提交 - DRAFT → SUBMITTED")
    void testSubmit_Success() {
        AbnormalContact existing = buildContact(1L, "YC-2024-001", AbnormalContactStatus.DRAFT);
        existing.setAffectSchedule(0);

        when(contactMapper.selectById(1L)).thenReturn(existing);
        when(contactMapper.updateById(any(AbnormalContact.class))).thenReturn(1);
        when(logMapper.insert(any(AbnormalContactLog.class))).thenReturn(1);

        contactService.submit(1L);

        verify(contactMapper).updateById(argThat(c -> {
            assertEquals(AbnormalContactStatus.SUBMITTED.getCode(), c.getStatus());
            assertNotNull(c.getPublishTime(), "提交时应记录发布时间");
            return true;
        }));
    }

    @Test
    @Order(21)
    @DisplayName("3.2 提交影响排程的异常 - 应触发 APS 重排")
    void testSubmit_AffectsSchedule() {
        AbnormalContact existing = buildContact(1L, "YC-2024-002", AbnormalContactStatus.DRAFT);
        existing.setAffectSchedule(1);
        existing.setEventCategory("EQUIPMENT_FAILURE");

        when(contactMapper.selectById(1L)).thenReturn(existing);
        when(contactMapper.updateById(any(AbnormalContact.class))).thenReturn(1);
        when(logMapper.insert(any(AbnormalContactLog.class))).thenReturn(1);

        contactService.submit(1L);

        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());
        List<ApplicationEvent> published = captor.getAllValues();
        assertTrue(published.stream().anyMatch(AbnormalSubmittedEvent.class::isInstance));
        assertTrue(published.stream().anyMatch(ApsSyncEvent.class::isInstance));
    }

    @Test
    @Order(22)
    @DisplayName("3.3 开始处理 - SUBMITTED → PROCESSING")
    void testProcess_Success() {
        AbnormalContact existing = buildContact(1L, "YC-2024-001", AbnormalContactStatus.SUBMITTED);

        when(contactMapper.selectById(1L)).thenReturn(existing);
        when(contactMapper.updateById(any(AbnormalContact.class))).thenReturn(1);
        when(logMapper.insert(any(AbnormalContactLog.class))).thenReturn(1);

        contactService.process(1L);

        verify(contactMapper).updateById(argThat(c ->
                AbnormalContactStatus.PROCESSING.getCode().equals(c.getStatus())));
    }

    @Test
    @Order(23)
    @DisplayName("3.4 DRAFT 状态直接处理 - 应拒绝")
    void testProcess_DraftStatus() {
        AbnormalContact existing = buildContact(1L, "YC-2024-001", AbnormalContactStatus.DRAFT);

        when(contactMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class, () -> contactService.process(1L),
                "草稿状态不应直接进入处理");
    }

    @Test
    @Order(24)
    @DisplayName("3.5 关闭 - PROCESSING → CLOSED")
    void testClose_Success() {
        AbnormalContact existing = buildContact(1L, "YC-2024-001", AbnormalContactStatus.PROCESSING);

        when(contactMapper.selectById(1L)).thenReturn(existing);
        when(contactMapper.updateById(any(AbnormalContact.class))).thenReturn(1);
        when(logMapper.insert(any(AbnormalContactLog.class))).thenReturn(1);

        contactService.close(1L);

        verify(contactMapper).updateById(argThat(c ->
                AbnormalContactStatus.CLOSED.getCode().equals(c.getStatus())));
    }

    @Test
    @Order(25)
    @DisplayName("3.6 SUBMITTED 状态直接关闭 - 应拒绝（必须先处理）")
    void testClose_SubmittedStatus() {
        AbnormalContact existing = buildContact(1L, "YC-2024-001", AbnormalContactStatus.SUBMITTED);

        when(contactMapper.selectById(1L)).thenReturn(existing);

        assertThrows(Exception.class, () -> contactService.close(1L),
                "未进入处理状态不应允许关闭");
    }

    // ==================== 4. 附件管理测试 ====================

    @Test
    @Order(30)
    @DisplayName("4.1 添加附件 - 应成功")
    void testAddAttachment_Success() {
        AbnormalContact contact = buildContact(1L, "YC-2024-001", AbnormalContactStatus.DRAFT);
        AbnormalContactAttachmentDTO dto = new AbnormalContactAttachmentDTO();
        dto.setFileName("异常照片.jpg");
        dto.setFileUrl("/uploads/abc.jpg");

        when(contactMapper.selectById(1L)).thenReturn(contact);
        when(attachmentMapper.insert(any(AbnormalContactAttachment.class))).thenReturn(1);

        contactService.addAttachment(1L, dto);

        verify(attachmentMapper).insert(argThat(a -> {
            assertEquals(0, a.getSigned(), "新附件初始应为未签署");
            return true;
        }));
    }

    @Test
    @Order(31)
    @DisplayName("4.2 删除未签署附件 - 应成功")
    void testDeleteAttachment_Unsigned() {
        AbnormalContactAttachment att = new AbnormalContactAttachment();
        att.setId(1L);
        att.setSigned(0);

        when(attachmentMapper.selectById(1L)).thenReturn(att);
        when(attachmentMapper.deleteById(1L)).thenReturn(1);

        contactService.deleteAttachment(1L);

        verify(attachmentMapper).deleteById(1L);
    }

    @Test
    @Order(32)
    @DisplayName("4.3 删除已签署附件 - 应拒绝")
    void testDeleteAttachment_Signed() {
        AbnormalContactAttachment att = new AbnormalContactAttachment();
        att.setId(1L);
        att.setSigned(1);

        when(attachmentMapper.selectById(1L)).thenReturn(att);

        assertThrows(Exception.class, () -> contactService.deleteAttachment(1L),
                "已签署的附件不应允许删除");
    }

    @Test
    @Order(33)
    @DisplayName("4.4 签署附件 - 应成功")
    void testSignAttachment_Success() {
        AbnormalContactAttachment att = new AbnormalContactAttachment();
        att.setId(1L);
        att.setSigned(0);

        when(attachmentMapper.selectById(1L)).thenReturn(att);
        when(attachmentMapper.updateById(any(AbnormalContactAttachment.class))).thenReturn(1);

        contactService.signAttachment(1L);

        verify(attachmentMapper).updateById(argThat(a -> {
            assertEquals(1, a.getSigned());
            assertNotNull(a.getSubmitTime());
            return true;
        }));
    }

    @Test
    @Order(34)
    @DisplayName("4.5 重复签署附件 - 应拒绝")
    void testSignAttachment_AlreadySigned() {
        AbnormalContactAttachment att = new AbnormalContactAttachment();
        att.setId(1L);
        att.setSigned(1);

        when(attachmentMapper.selectById(1L)).thenReturn(att);

        assertThrows(Exception.class, () -> contactService.signAttachment(1L),
                "已签署的附件不应重复签署");
    }

    // ==================== 5. 状态枚举测试 ====================

    @Test
    @Order(40)
    @DisplayName("5.1 AbnormalContactStatus 枚举完整性")
    void testAbnormalContactStatusEnum() {
        assertNotNull(AbnormalContactStatus.DRAFT);
        assertNotNull(AbnormalContactStatus.SUBMITTED);
        assertNotNull(AbnormalContactStatus.PROCESSING);
        assertNotNull(AbnormalContactStatus.CLOSED);
    }

    // ==================== 辅助方法 ====================

    private AbnormalContactDTO buildContactDTO(String contactNo, String subject) {
        AbnormalContactDTO dto = new AbnormalContactDTO();
        dto.setContactNo(contactNo);
        dto.setSubject(subject);
        dto.setEventCategory("EQUIPMENT_FAILURE");
        return dto;
    }

    private AbnormalContact buildContact(Long id, String contactNo, AbnormalContactStatus status) {
        AbnormalContact c = new AbnormalContact();
        c.setId(id);
        c.setContactNo(contactNo);
        c.setStatus(status.getCode());
        return c;
    }
}
