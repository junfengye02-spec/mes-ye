package com.mes.dispatch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.dispatch.domain.entity.DispatchAssignment;
import com.mes.dispatch.domain.entity.DispatchTask;
import com.mes.dispatch.domain.query.DispatchTaskQuery;
import com.mes.dispatch.domain.vo.DispatchAssignmentVO;
import com.mes.dispatch.domain.vo.DispatchTaskVO;
import com.mes.dispatch.enums.AssignmentStatus;
import com.mes.dispatch.enums.DispatchStatus;
import com.mes.dispatch.mapper.DispatchAssignmentMapper;
import com.mes.dispatch.mapper.DispatchTaskMapper;
import com.mes.dispatch.service.IDispatchTaskService;
import com.mes.workorder.domain.entity.WorkOrder;
import com.mes.workorder.domain.entity.WorkOrderTask;
import com.mes.workorder.mapper.WorkOrderMapper;
import com.mes.workorder.mapper.WorkOrderTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 派工任务 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchTaskServiceImpl extends ServiceImpl<DispatchTaskMapper, DispatchTask>
        implements IDispatchTaskService {

    private final DispatchAssignmentMapper assignmentMapper;
    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderTaskMapper workOrderTaskMapper;

    @Override
    public PageResult<DispatchTaskVO> page(DispatchTaskQuery query) {
        LambdaQueryWrapper<DispatchTask> wrapper = new LambdaQueryWrapper<DispatchTask>()
                .eq(query.getWorkOrderId() != null,
                        DispatchTask::getWorkOrderId, query.getWorkOrderId())
                .like(StringUtils.hasText(query.getOrderNo()),
                        DispatchTask::getOrderNo, query.getOrderNo())
                .eq(StringUtils.hasText(query.getProcessNo()),
                        DispatchTask::getProcessNo, query.getProcessNo())
                .eq(StringUtils.hasText(query.getDispatchStatus()),
                        DispatchTask::getDispatchStatus, query.getDispatchStatus())
                .orderByDesc(DispatchTask::getCreatedTime);

        Page<DispatchTask> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<DispatchTaskVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public DispatchTaskVO getDetail(Long id) {
        DispatchTask entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        DispatchTaskVO vo = toVO(entity);

        // 查询分配记录
        List<DispatchAssignment> assignments = assignmentMapper.selectList(
                new LambdaQueryWrapper<DispatchAssignment>()
                        .eq(DispatchAssignment::getDispatchTaskId, id)
                        .orderByDesc(DispatchAssignment::getAssignedTime));
        vo.setAssignments(assignments.stream().map(a -> {
            DispatchAssignmentVO avo = new DispatchAssignmentVO();
            BeanUtils.copyProperties(a, avo);
            return avo;
        }).toList());

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateFromWorkOrder(Long workOrderId) {
        // 校验工单存在
        WorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        AssertUtil.notNull(workOrder, "工单不存在");

        // 查询工单的工作清单
        List<WorkOrderTask> tasks = workOrderTaskMapper.selectList(
                new LambdaQueryWrapper<WorkOrderTask>()
                        .eq(WorkOrderTask::getWorkOrderId, workOrderId)
                        .orderByAsc(WorkOrderTask::getSequenceNo));
        AssertUtil.isTrue(!tasks.isEmpty(), "工单没有工作清单，无法生成派工任务");

        // 检查是否已生成过派工任务（防止重复）
        long existCount = count(new LambdaQueryWrapper<DispatchTask>()
                .eq(DispatchTask::getWorkOrderId, workOrderId));
        AssertUtil.isFalse(existCount > 0, "该工单已生成派工任务，不可重复生成");

        // 为每个工作清单生成派工任务
        for (WorkOrderTask task : tasks) {
            DispatchTask dispatchTask = new DispatchTask();
            dispatchTask.setWorkOrderId(workOrderId);
            dispatchTask.setWorkOrderTaskId(task.getId());
            dispatchTask.setOrderNo(workOrder.getOrderNo());
            dispatchTask.setProcessNo(task.getTaskNo());
            dispatchTask.setWorkName(task.getTaskName());
            dispatchTask.setPlanWorkCenterId(task.getPlanWorkCenterId());
            dispatchTask.setSerialNo(task.getSerialNo());
            dispatchTask.setProjectName(task.getProjectName());
            dispatchTask.setPlanQty(task.getPlanQty());
            dispatchTask.setQtyUnit(task.getQtyUnit());
            dispatchTask.setDispatchStatus(DispatchStatus.UNASSIGNED.getCode());
            dispatchTask.setCreatedTime(LocalDateTime.now());
            dispatchTask.setUpdatedTime(LocalDateTime.now());
            save(dispatchTask);
        }

        log.info("从工单 {} 生成 {} 个派工任务", workOrder.getWorkOrderNo(), tasks.size());
    }

    private DispatchTaskVO toVO(DispatchTask entity) {
        DispatchTaskVO vo = new DispatchTaskVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
