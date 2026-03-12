package com.mes.query.service;

import com.mes.query.domain.dto.ShiftHandoverAttachmentDTO;
import com.mes.query.domain.vo.ShiftHandoverAttachmentVO;
import java.util.List;

public interface IShiftHandoverAttachmentService {
    List<ShiftHandoverAttachmentVO> listByHandoverId(Long handoverId);
    Long create(ShiftHandoverAttachmentDTO dto);
    void delete(Long id);
}
