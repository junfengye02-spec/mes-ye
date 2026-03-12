package com.mes.process.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.process.domain.dto.ProcessTemplateDTO;
import com.mes.process.domain.entity.ProcessTemplate;
import com.mes.process.domain.query.ProcessTemplateQuery;
import com.mes.process.domain.vo.ProcessTemplateVO;

import java.util.List;

/**
 * 工序模板 Service 接口
 */
public interface IProcessTemplateService extends IService<ProcessTemplate> {

    /** 分页查询 */
    PageResult<ProcessTemplateVO> page(ProcessTemplateQuery query);

    /** 树形结构查询 */
    List<ProcessTemplateVO> tree();

    /** 获取详情 */
    ProcessTemplateVO getDetail(Long id);

    /** 新增 */
    Long create(ProcessTemplateDTO dto);

    /** 修改 */
    void update(Long id, ProcessTemplateDTO dto);

    /** 删除（需检查子工序引用） */
    void delete(Long id);
}
