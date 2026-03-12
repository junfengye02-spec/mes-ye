package com.mes.workorder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.workorder.domain.dto.WorkOrderAttachmentDTO;
import com.mes.workorder.domain.entity.WorkOrderAttachment;
import com.mes.workorder.domain.vo.WorkOrderAttachmentVO;

import java.util.List;

public interface IWorkOrderAttachmentService extends IService<WorkOrderAttachment> {

    List<WorkOrderAttachmentVO> listByWorkOrderId(Long workOrderId);

    Long create(Long workOrderId, WorkOrderAttachmentDTO dto);

    void deleteAttachment(Long id);
}
