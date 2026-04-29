package com.mes.dispatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.common.core.PageResult;
import com.mes.dispatch.domain.dto.DispatchTaskAssignDTO;
import com.mes.dispatch.domain.dto.DispatchTaskCompleteDTO;
import com.mes.dispatch.domain.dto.DispatchTaskCreateDTO;
import com.mes.dispatch.domain.dto.DispatchTaskUpdateDTO;
import com.mes.dispatch.domain.entity.DispatchTask;
import com.mes.dispatch.domain.query.DispatchTaskQuery;
import com.mes.dispatch.domain.vo.DispatchTaskVO;

/**
 * 派工任务 Service
 */
public interface IDispatchTaskService extends IService<DispatchTask> {

    PageResult<DispatchTaskVO> page(DispatchTaskQuery query);

    DispatchTaskVO getDetail(Long id);

    /**
     * 从工单工作清单自动生成派工任务
     */
    void generateFromWorkOrder(Long workOrderId);

    // ==================== P0-03 新增写接口 ====================

    /**
     * 手动创建派工任务
     *
     * @param dto 创建参数
     * @return 新建派工任务 ID
     */
    Long create(DispatchTaskCreateDTO dto);

    /**
     * 更新派工任务基本信息（仅允许未开工状态修改）
     *
     * @param dto 更新参数
     */
    void update(DispatchTaskUpdateDTO dto);

    /**
     * 撤销派工任务（转为 CANCELLED 状态，必须记录撤销原因）
     *
     * @param id           派工任务 ID
     * @param cancelReason 撤销原因
     */
    void cancel(Long id, String cancelReason);

    /**
     * 派工（按 PERSON/EQUIPMENT/TEAM 批量指派）
     * <p>会做资源占用冲突校验：同一 assigneeId 在时间段重叠的 ACTIVE 派工不允许重复指派</p>
     *
     * @param dto 指派参数
     */
    void assign(DispatchTaskAssignDTO dto);

    /**
     * 取消指派（撤销单条分配记录）
     *
     * @param assignmentId 分配记录 ID（对应 mes_dispatch_assignment.id）
     * @param reason       取消原因
     */
    void unassign(Long assignmentId, String reason);

    /**
     * 开工（状态 ASSIGNED &rarr; IN_PROGRESS，记录实际开工时间）
     *
     * @param id 派工任务 ID
     */
    void start(Long id);

    /**
     * 完工（状态 IN_PROGRESS &rarr; COMPLETED，记录实际数量与质量结果）
     *
     * @param id  派工任务 ID
     * @param dto 完工参数
     */
    void complete(Long id, DispatchTaskCompleteDTO dto);
}
