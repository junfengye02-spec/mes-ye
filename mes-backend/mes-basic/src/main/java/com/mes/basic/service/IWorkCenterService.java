package com.mes.basic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.basic.domain.dto.WorkCenterDTO;
import com.mes.basic.domain.entity.WorkCenter;
import com.mes.basic.domain.query.WorkCenterQuery;
import com.mes.basic.domain.vo.WorkCenterVO;
import com.mes.common.core.PageResult;

/**
 * 工作中心 Service 接口
 */
public interface IWorkCenterService extends IService<WorkCenter> {

    /**
     * 分页查询工作中心
     */
    PageResult<WorkCenterVO> page(WorkCenterQuery query);

    /**
     * 获取工作中心详情
     */
    WorkCenterVO getDetail(Long id);

    /**
     * 新增工作中心
     */
    Long create(WorkCenterDTO dto);

    /**
     * 修改工作中心
     */
    void update(Long id, WorkCenterDTO dto);

    /**
     * 删除工作中心
     */
    void delete(Long id);

    /**
     * 批量编辑工作中心
     */
    void batchUpdate(java.util.List<WorkCenterDTO> dtoList, java.util.List<Long> ids);
}
