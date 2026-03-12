package com.mes.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.framework.security.SecurityUtils;
import com.mes.workorder.domain.dto.WorkOrderAttachmentDTO;
import com.mes.workorder.domain.entity.WorkOrderAttachment;
import com.mes.workorder.domain.vo.WorkOrderAttachmentVO;
import com.mes.workorder.mapper.WorkOrderAttachmentMapper;
import com.mes.workorder.service.IWorkOrderAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkOrderAttachmentServiceImpl
        extends ServiceImpl<WorkOrderAttachmentMapper, WorkOrderAttachment>
        implements IWorkOrderAttachmentService {

    @Override
    public List<WorkOrderAttachmentVO> listByWorkOrderId(Long workOrderId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<WorkOrderAttachment>()
                        .eq(WorkOrderAttachment::getWorkOrderId, workOrderId))
                .stream().map(e -> {
                    WorkOrderAttachmentVO vo = new WorkOrderAttachmentVO();
                    BeanUtils.copyProperties(e, vo);
                    return vo;
                }).toList();
    }

    @Override
    public Long create(Long workOrderId, WorkOrderAttachmentDTO dto) {
        WorkOrderAttachment entity = new WorkOrderAttachment();
        BeanUtils.copyProperties(dto, entity);
        entity.setWorkOrderId(workOrderId);
        String currentUser = Optional.ofNullable(SecurityUtils.getCurrentUsername()).orElse("system");
        entity.setCreatedBy(currentUser);
        entity.setCreatedTime(LocalDateTime.now());
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void deleteAttachment(Long id) {
        baseMapper.deleteById(id);
    }
}
