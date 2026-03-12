package com.mes.dispatch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.dispatch.domain.dto.DispatchAssignDTO;
import com.mes.dispatch.domain.entity.DispatchAssignment;
import com.mes.dispatch.domain.entity.DispatchTask;
import com.mes.dispatch.domain.vo.DispatchAssignmentVO;
import com.mes.dispatch.enums.AssignType;
import com.mes.dispatch.enums.AssignmentStatus;
import com.mes.dispatch.enums.DispatchStatus;
import com.mes.dispatch.mapper.DispatchAssignmentMapper;
import com.mes.dispatch.mapper.DispatchTaskMapper;
import com.mes.dispatch.service.IDispatchAssignmentService;
import com.mes.dispatch.service.IDispatchStatusLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 派工分配 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchAssignmentServiceImpl implements IDispatchAssignmentService {

    private final DispatchAssignmentMapper assignmentMapper;
    private final DispatchTaskMapper taskMapper;
    private final IDispatchStatusLogService statusLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPerson(Long taskId, DispatchAssignDTO dto) {
        doAssign(taskId, AssignType.PERSON.getCode(), dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDevice(Long taskId, DispatchAssignDTO dto) {
        doAssign(taskId, AssignType.DEVICE.getCode(), dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTeam(Long taskId, DispatchAssignDTO dto) {
        doAssign(taskId, AssignType.TEAM.getCode(), dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long assignmentId, String reason) {
        DispatchAssignment assignment = assignmentMapper.selectById(assignmentId);
        AssertUtil.notNull(assignment, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(AssignmentStatus.ACTIVE.getCode().equals(assignment.getStatus()),
                "仅有效状态的分配可以撤销");

        // 更新分配记录状态
        assignment.setStatus(AssignmentStatus.REVOKED.getCode());
        assignment.setRevokedBy("system"); // TODO: 从 SecurityContext 获取
        assignment.setRevokedTime(LocalDateTime.now());
        assignmentMapper.updateById(assignment);

        // 检查该任务是否还有其他有效分配
        long activeCount = assignmentMapper.selectCount(
                new LambdaQueryWrapper<DispatchAssignment>()
                        .eq(DispatchAssignment::getDispatchTaskId, assignment.getDispatchTaskId())
                        .eq(DispatchAssignment::getStatus, AssignmentStatus.ACTIVE.getCode()));

        // 如果没有其他有效分配，任务状态回退为未分派
        if (activeCount == 0) {
            DispatchTask task = taskMapper.selectById(assignment.getDispatchTaskId());
            if (task != null) {
                String fromStatus = task.getDispatchStatus();
                task.setDispatchStatus(DispatchStatus.UNASSIGNED.getCode());
                task.setUpdatedTime(LocalDateTime.now());
                taskMapper.updateById(task);

                statusLogService.log(task.getId(), fromStatus,
                        DispatchStatus.UNASSIGNED.getCode(), "撤销分派",
                        "撤销分派，原因: " + reason);
            }
        }

        log.info("撤销分派: assignmentId={}, reason={}", assignmentId, reason);
    }

    @Override
    public List<DispatchAssignmentVO> listByTaskId(Long taskId) {
        List<DispatchAssignment> list = assignmentMapper.selectList(
                new LambdaQueryWrapper<DispatchAssignment>()
                        .eq(DispatchAssignment::getDispatchTaskId, taskId)
                        .orderByDesc(DispatchAssignment::getAssignedTime));

        return list.stream().map(entity -> {
            DispatchAssignmentVO vo = new DispatchAssignmentVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).toList();
    }

    // ==================== 私有方法 ====================

    private void doAssign(Long taskId, String assignType, DispatchAssignDTO dto) {
        // 校验任务存在
        DispatchTask task = taskMapper.selectById(taskId);
        AssertUtil.notNull(task, "派工任务不存在");

        // 仅未分派或已撤销状态可以派工
        AssertUtil.isTrue(
                DispatchStatus.UNASSIGNED.getCode().equals(task.getDispatchStatus()) ||
                DispatchStatus.REVOKED.getCode().equals(task.getDispatchStatus()),
                "仅未分派或已撤销状态的任务可以派工");

        // 创建分配记录
        DispatchAssignment assignment = new DispatchAssignment();
        assignment.setDispatchTaskId(taskId);
        assignment.setAssignType(assignType);
        assignment.setAssigneeId(dto.getAssigneeId());
        assignment.setAssigneeCode(dto.getAssigneeCode());
        assignment.setAssigneeName(dto.getAssigneeName());
        assignment.setAssignedQty(dto.getAssignedQty());
        assignment.setQtyUnit(dto.getQtyUnit());
        assignment.setStatus(AssignmentStatus.ACTIVE.getCode());
        assignment.setAssignedBy("system"); // TODO: 从 SecurityContext 获取
        assignment.setAssignedTime(LocalDateTime.now());
        assignmentMapper.insert(assignment);

        // 更新任务状态为已分派
        String fromStatus = task.getDispatchStatus();
        task.setDispatchStatus(DispatchStatus.ASSIGNED.getCode());
        task.setUpdatedTime(LocalDateTime.now());
        taskMapper.updateById(task);

        statusLogService.log(taskId, fromStatus, DispatchStatus.ASSIGNED.getCode(),
                assignType + " 分派",
                "分派给 " + dto.getAssigneeCode() + " " + dto.getAssigneeName());

        log.info("派工分派: taskId={}, type={}, assignee={}", taskId, assignType, dto.getAssigneeCode());
    }
}
