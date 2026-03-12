package com.mes.process.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.process.domain.dto.ProcessInfoDTO;
import com.mes.process.domain.entity.ProcessInfo;
import com.mes.process.domain.query.ProcessInfoQuery;
import com.mes.process.domain.vo.ProcessInfoVO;

import java.util.List;

/**
 * 工序信息 Service 接口
 */
public interface IProcessInfoService extends IService<ProcessInfo> {

    /** 分页查询 */
    PageResult<ProcessInfoVO> page(ProcessInfoQuery query);

    /** 获取详情 */
    ProcessInfoVO getDetail(Long id);

    /** 新增 */
    Long create(ProcessInfoDTO dto);

    /** 修改 */
    void update(Long id, ProcessInfoDTO dto);

    /** 删除 */
    void delete(Long id);

    /** 批量编辑 */
    void batchUpdate(List<Long> ids, List<ProcessInfoDTO> dtoList);
}
