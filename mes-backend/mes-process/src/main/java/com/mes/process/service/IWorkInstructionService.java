package com.mes.process.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.process.domain.dto.WorkInstructionDTO;
import com.mes.process.domain.entity.WorkInstruction;
import com.mes.process.domain.query.WorkInstructionQuery;
import com.mes.process.domain.vo.WorkInstructionVO;

/**
 * 指导书 Service 接口
 */
public interface IWorkInstructionService extends IService<WorkInstruction> {

    /** 分页查询 */
    PageResult<WorkInstructionVO> page(WorkInstructionQuery query);

    /** 获取详情（含人员列表） */
    WorkInstructionVO getDetail(Long id);

    /** 新增（含人员列表） */
    Long create(WorkInstructionDTO dto);

    /** 修改（含人员列表） */
    void update(Long id, WorkInstructionDTO dto);

    /** 删除（级联删除人员） */
    void delete(Long id);
}
