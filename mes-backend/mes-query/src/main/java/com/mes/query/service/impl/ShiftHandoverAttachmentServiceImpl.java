package com.mes.query.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.common.utils.AssertUtil;
import com.mes.query.domain.dto.ShiftHandoverAttachmentDTO;
import com.mes.query.domain.entity.ShiftHandoverAttachment;
import com.mes.query.domain.vo.ShiftHandoverAttachmentVO;
import com.mes.query.mapper.ShiftHandoverAttachmentMapper;
import com.mes.query.service.IShiftHandoverAttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftHandoverAttachmentServiceImpl implements IShiftHandoverAttachmentService {

    private final ShiftHandoverAttachmentMapper attachmentMapper;

    @Override
    public List<ShiftHandoverAttachmentVO> listByHandoverId(Long handoverId) {
        List<ShiftHandoverAttachment> list = attachmentMapper.selectList(
                new LambdaQueryWrapper<ShiftHandoverAttachment>()
                        .eq(ShiftHandoverAttachment::getHandoverId, handoverId)
                        .orderByAsc(ShiftHandoverAttachment::getCreatedTime));
        return list.stream().map(e -> {
            ShiftHandoverAttachmentVO vo = new ShiftHandoverAttachmentVO();
            BeanUtils.copyProperties(e, vo);
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ShiftHandoverAttachmentDTO dto) {
        ShiftHandoverAttachment entity = new ShiftHandoverAttachment();
        BeanUtils.copyProperties(dto, entity);
        entity.setUploader("system"); // TODO: SecurityContext
        entity.setDownloadCount(0);
        entity.setLoadStatus("LOADED");
        entity.setCreatedBy("system");
        entity.setCreatedTime(LocalDateTime.now());
        attachmentMapper.insert(entity);

        log.info("新增交班附件: handoverId={}, fileName={}", dto.getHandoverId(), dto.getFileName());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ShiftHandoverAttachment entity = attachmentMapper.selectById(id);
        AssertUtil.notNull(entity, "附件不存在");
        attachmentMapper.deleteById(id);
        log.info("删除交班附件: id={}", id);
    }
}
