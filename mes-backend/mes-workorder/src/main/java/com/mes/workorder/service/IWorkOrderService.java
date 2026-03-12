package com.mes.workorder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.workorder.domain.dto.WorkOrderDTO;
import com.mes.workorder.domain.entity.WorkOrder;
import com.mes.workorder.domain.query.WorkOrderQuery;
import com.mes.workorder.domain.vo.WorkOrderVO;

/**
 * 生产工单 Service
 */
public interface IWorkOrderService extends IService<WorkOrder> {

    PageResult<WorkOrderVO> page(WorkOrderQuery query);

    WorkOrderVO getDetail(Long id);

    Long create(WorkOrderDTO dto);

    void update(Long id, WorkOrderDTO dto);

    void delete(Long id);

    /** 下发 */
    void release(Long id);

    /** 开工 */
    void start(Long id);

    /** 完工 */
    void complete(Long id);

    /** 强制完工 */
    void forceComplete(Long id, String reason);
}
