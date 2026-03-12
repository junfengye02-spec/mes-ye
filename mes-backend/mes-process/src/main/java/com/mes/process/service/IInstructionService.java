package com.mes.process.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageQuery;
import com.mes.common.core.PageResult;
import com.mes.process.domain.dto.InstructionDTO;
import com.mes.process.domain.entity.Instruction;
import com.mes.process.domain.query.InstructionQuery;
import com.mes.process.domain.vo.InstructionFlowLogVO;
import com.mes.process.domain.vo.InstructionVO;

/**
 * 指示书 Service 接口
 */
public interface IInstructionService extends IService<Instruction> {

    /** 分页查询 */
    PageResult<InstructionVO> page(InstructionQuery query);

    /** 获取详情（含阶段+序列号子表） */
    InstructionVO getDetail(Long id);

    /** 新增（含阶段+序列号子表） */
    Long create(InstructionDTO dto);

    /** 修改（含子表更新） */
    void update(Long id, InstructionDTO dto);

    /** 删除 */
    void delete(Long id);

    /** 版本升级：深拷贝当前版本，生成新版本 */
    Long upgrade(Long id);

    /** 查询流程日志 */
    PageResult<InstructionFlowLogVO> getFlowLogs(Long id, PageQuery query);
}
