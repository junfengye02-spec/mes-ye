package com.mes.process.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.process.domain.dto.MachiningProgramDTO;
import com.mes.process.domain.entity.MachiningProgram;
import com.mes.process.domain.query.MachiningProgramQuery;
import com.mes.process.domain.vo.MachiningProgramVO;

/**
 * 机械加工程序 Service 接口
 */
public interface IMachiningProgramService extends IService<MachiningProgram> {

    /** 分页查询 */
    PageResult<MachiningProgramVO> page(MachiningProgramQuery query);

    /** 获取详情 */
    MachiningProgramVO getDetail(Long id);

    /** 新增 */
    Long create(MachiningProgramDTO dto);

    /** 修改 */
    void update(Long id, MachiningProgramDTO dto);

    /** 删除 */
    void delete(Long id);
}
