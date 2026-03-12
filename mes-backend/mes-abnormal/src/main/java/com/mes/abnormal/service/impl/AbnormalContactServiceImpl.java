package com.mes.abnormal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.abnormal.domain.dto.AbnormalContactAttachmentDTO;
import com.mes.abnormal.domain.dto.AbnormalContactDTO;
import com.mes.abnormal.domain.entity.AbnormalContact;
import com.mes.abnormal.domain.entity.AbnormalContactAttachment;
import com.mes.abnormal.domain.entity.AbnormalContactLog;
import com.mes.abnormal.domain.query.AbnormalContactQuery;
import com.mes.abnormal.domain.vo.AbnormalContactAttachmentVO;
import com.mes.abnormal.domain.vo.AbnormalContactLogVO;
import com.mes.abnormal.domain.vo.AbnormalContactVO;
import com.mes.abnormal.enums.AbnormalContactStatus;
import com.mes.abnormal.mapper.AbnormalContactAttachmentMapper;
import com.mes.abnormal.mapper.AbnormalContactLogMapper;
import com.mes.abnormal.mapper.AbnormalContactMapper;
import com.mes.abnormal.service.IAbnormalContactService;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.common.event.ApsSyncEvent;
import com.mes.common.utils.NumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异常联络单 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AbnormalContactServiceImpl extends ServiceImpl<AbnormalContactMapper, AbnormalContact>
        implements IAbnormalContactService {

    private final AbnormalContactAttachmentMapper attachmentMapper;
    private final AbnormalContactLogMapper logMapper;
    private final ApplicationEventPublisher eventPublisher;

    /** APS 重排防抖：key=eventCategory, value=上次触发时间戳 */
    private final ConcurrentHashMap<String, Long> lastTriggerTime = new ConcurrentHashMap<>();
    private static final long DEBOUNCE_INTERVAL_MS = 5 * 60 * 1000L;

    @Override
    public PageResult<AbnormalContactVO> page(AbnormalContactQuery query) {
        LambdaQueryWrapper<AbnormalContact> wrapper = new LambdaQueryWrapper<AbnormalContact>()
                .like(StringUtils.hasText(query.getContactNo()),
                        AbnormalContact::getContactNo, query.getContactNo())
                .like(StringUtils.hasText(query.getSubject()),
                        AbnormalContact::getSubject, query.getSubject())
                .eq(StringUtils.hasText(query.getEventCategory()),
                        AbnormalContact::getEventCategory, query.getEventCategory())
                .eq(StringUtils.hasText(query.getStatus()),
                        AbnormalContact::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getOrderNo()),
                        AbnormalContact::getOrderNo, query.getOrderNo())
                .eq(StringUtils.hasText(query.getInitiateDept()),
                        AbnormalContact::getInitiateDept, query.getInitiateDept())
                .orderByDesc(AbnormalContact::getCreatedTime);

        Page<AbnormalContact> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<AbnormalContactVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public AbnormalContactVO getDetail(Long id) {
        AbnormalContact entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        AbnormalContactVO vo = toVO(entity);

        // 查询附件
        vo.setAttachments(listAttachments(id));

        // 查询状态日志
        List<AbnormalContactLog> logs = logMapper.selectList(
                new LambdaQueryWrapper<AbnormalContactLog>()
                        .eq(AbnormalContactLog::getContactId, id)
                        .orderByDesc(AbnormalContactLog::getOperatedTime));
        vo.setLogs(logs.stream().map(this::toLogVO).toList());

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AbnormalContactDTO dto) {
        AbnormalContact entity = new AbnormalContact();
        BeanUtils.copyProperties(dto, entity);

        // 自动生成单号（如果未手工填写）
        if (!StringUtils.hasText(entity.getContactNo())) {
            entity.setContactNo(NumberGenerator.generate("YC"));
        } else {
            // 校验唯一性
            long count = count(new LambdaQueryWrapper<AbnormalContact>()
                    .eq(AbnormalContact::getContactNo, entity.getContactNo()));
            AssertUtil.isFalse(count > 0, "异常联络单号已存在: " + entity.getContactNo());
        }

        entity.setStatus(AbnormalContactStatus.DRAFT.getCode());
        save(entity);

        addLog(entity.getId(), null, AbnormalContactStatus.DRAFT.getCode(), "创建",
                "创建异常联络单 " + entity.getContactNo());

        log.info("新增异常联络单: {}", entity.getContactNo());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AbnormalContactDTO dto) {
        AbnormalContact existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(AbnormalContactStatus.DRAFT.getCode().equals(existing.getStatus()),
                "仅草稿状态的异常联络单可以编辑");

        String status = existing.getStatus();
        String contactNo = existing.getContactNo();
        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        existing.setStatus(status);
        existing.setContactNo(contactNo); // 单号不允许修改
        updateById(existing);

        log.info("修改异常联络单: {}", existing.getContactNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AbnormalContact entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(AbnormalContactStatus.DRAFT.getCode().equals(entity.getStatus()),
                "仅草稿状态的异常联络单可以删除");

        // 删除附件
        attachmentMapper.delete(new LambdaQueryWrapper<AbnormalContactAttachment>()
                .eq(AbnormalContactAttachment::getContactId, id));

        // 删除日志
        logMapper.delete(new LambdaQueryWrapper<AbnormalContactLog>()
                .eq(AbnormalContactLog::getContactId, id));

        removeById(id);
        log.info("删除异常联络单: {}", entity.getContactNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        AbnormalContact entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(AbnormalContactStatus.DRAFT.getCode().equals(entity.getStatus()),
                "仅草稿状态的异常联络单可以提交");

        String fromStatus = entity.getStatus();
        entity.setStatus(AbnormalContactStatus.SUBMITTED.getCode());
        entity.setPublishTime(LocalDateTime.now());
        updateById(entity);

        addLog(id, fromStatus, AbnormalContactStatus.SUBMITTED.getCode(), "提交",
                "异常联络单 " + entity.getContactNo() + " 已提交");

        // 如果影响排程，触发 APS 重排（带防抖）
        if (Integer.valueOf(1).equals(entity.getAffectSchedule())) {
            triggerApsReschedule(entity);
        }

        log.info("异常联络单提交: {}", entity.getContactNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(Long id) {
        AbnormalContact entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(AbnormalContactStatus.SUBMITTED.getCode().equals(entity.getStatus()),
                "仅已提交状态的异常联络单可以开始处理");

        String fromStatus = entity.getStatus();
        entity.setStatus(AbnormalContactStatus.PROCESSING.getCode());
        updateById(entity);

        addLog(id, fromStatus, AbnormalContactStatus.PROCESSING.getCode(), "处理",
                "异常联络单 " + entity.getContactNo() + " 开始处理");

        log.info("异常联络单开始处理: {}", entity.getContactNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id) {
        AbnormalContact entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(AbnormalContactStatus.PROCESSING.getCode().equals(entity.getStatus()),
                "仅处理中状态的异常联络单可以关闭");

        String fromStatus = entity.getStatus();
        entity.setStatus(AbnormalContactStatus.CLOSED.getCode());
        updateById(entity);

        addLog(id, fromStatus, AbnormalContactStatus.CLOSED.getCode(), "关闭",
                "异常联络单 " + entity.getContactNo() + " 已关闭");

        log.info("异常联络单关闭: {}", entity.getContactNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addAttachment(Long contactId, AbnormalContactAttachmentDTO dto) {
        AbnormalContact contact = getById(contactId);
        AssertUtil.notNull(contact, "异常联络单不存在");

        AbnormalContactAttachment attachment = new AbnormalContactAttachment();
        BeanUtils.copyProperties(dto, attachment);
        attachment.setContactId(contactId);
        attachment.setSigned(0);
        attachment.setCreatedTime(LocalDateTime.now());
        attachment.setUpdatedTime(LocalDateTime.now());
        attachmentMapper.insert(attachment);

        log.info("新增异常联络单附件: contactId={}, fileName={}", contactId, dto.getFileName());
        return attachment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttachment(Long attachmentId) {
        AbnormalContactAttachment attachment = attachmentMapper.selectById(attachmentId);
        AssertUtil.notNull(attachment, "附件不存在");
        AssertUtil.isFalse(Integer.valueOf(1).equals(attachment.getSigned()), "已签署的附件不可删除");

        attachmentMapper.deleteById(attachmentId);
        log.info("删除异常联络单附件: id={}", attachmentId);
    }

    @Override
    public List<AbnormalContactAttachmentVO> listAttachments(Long contactId) {
        List<AbnormalContactAttachment> list = attachmentMapper.selectList(
                new LambdaQueryWrapper<AbnormalContactAttachment>()
                        .eq(AbnormalContactAttachment::getContactId, contactId)
                        .orderByAsc(AbnormalContactAttachment::getCreatedTime));

        return list.stream().map(this::toAttachmentVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signAttachment(Long attachmentId) {
        AbnormalContactAttachment attachment = attachmentMapper.selectById(attachmentId);
        AssertUtil.notNull(attachment, "附件不存在");
        AssertUtil.isFalse(Integer.valueOf(1).equals(attachment.getSigned()), "附件已签署，不可重复签署");

        attachment.setSigned(1);
        attachment.setSubmitTime(LocalDateTime.now());
        attachment.setUpdatedTime(LocalDateTime.now());
        attachmentMapper.updateById(attachment);

        log.info("签署异常联络单附件: id={}", attachmentId);
    }

    // ==================== APS 防抖触发 ====================

    private void triggerApsReschedule(AbnormalContact contact) {
        String category = contact.getEventCategory();
        if (!StringUtils.hasText(category)) {
            category = "DEFAULT";
        }

        long now = System.currentTimeMillis();
        Long last = lastTriggerTime.get(category);
        if (last != null && (now - last) < DEBOUNCE_INTERVAL_MS) {
            log.info("APS重排防抖: 事件分类={}, 距上次触发未满5分钟，跳过", category);
            return;
        }
        lastTriggerTime.put(category, now);

        // 发布 APS 同步事件，由 mes-aps 模块的 ApsSyncEventListener 监听并触发重排
        log.info("触发APS重排: 异常联络单={}, 事件分类={}", contact.getContactNo(), category);
        try {
            String payload = String.format(
                    "{\"contactNo\":\"%s\",\"eventCategory\":\"%s\",\"reason\":\"%s\"}",
                    contact.getContactNo(), category,
                    contact.getAbnormalDesc() != null ? contact.getAbnormalDesc() : "");
            eventPublisher.publishEvent(new ApsSyncEvent(
                    this, "ABNORMAL", category,
                    contact.getId(), contact.getContactNo(), 1, payload));
        } catch (Exception e) {
            log.warn("发布APS重排事件失败（不影响业务）: {}", e.getMessage());
        }
    }

    // ==================== 日志记录 ====================

    private void addLog(Long contactId, String fromStatus, String toStatus, String action, String remark) {
        AbnormalContactLog logEntity = new AbnormalContactLog();
        logEntity.setContactId(contactId);
        logEntity.setFromStatus(fromStatus);
        logEntity.setToStatus(toStatus);
        logEntity.setAction(action);
        logEntity.setOperator("system"); // TODO: 从 SecurityContext 获取
        logEntity.setOperatedTime(LocalDateTime.now());
        logEntity.setRemark(remark);
        logMapper.insert(logEntity);
    }

    // ==================== 转换方法 ====================

    private AbnormalContactVO toVO(AbnormalContact entity) {
        AbnormalContactVO vo = new AbnormalContactVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private AbnormalContactAttachmentVO toAttachmentVO(AbnormalContactAttachment entity) {
        AbnormalContactAttachmentVO vo = new AbnormalContactAttachmentVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private AbnormalContactLogVO toLogVO(AbnormalContactLog entity) {
        AbnormalContactLogVO vo = new AbnormalContactLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
