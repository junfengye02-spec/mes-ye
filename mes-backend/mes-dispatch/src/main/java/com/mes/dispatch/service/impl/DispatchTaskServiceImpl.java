package com.mes.dispatch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.event.DispatchAllTasksCompletedEvent;
import com.mes.common.event.DispatchTaskCompletedEvent;
import com.mes.common.event.DispatchTaskQualityFailedEvent;
import com.mes.common.event.DispatchTaskStartedEvent;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.dispatch.domain.dto.DispatchAssignDTO;
import com.mes.dispatch.domain.dto.DispatchTaskAssignDTO;
import com.mes.dispatch.domain.dto.DispatchTaskCompleteDTO;
import com.mes.dispatch.domain.dto.DispatchTaskCreateDTO;
import com.mes.dispatch.domain.dto.DispatchTaskUpdateDTO;
import com.mes.dispatch.domain.entity.DispatchAssignment;
import com.mes.dispatch.domain.entity.DispatchTask;
import com.mes.dispatch.domain.query.DispatchTaskQuery;
import com.mes.dispatch.domain.vo.DispatchAssignmentVO;
import com.mes.dispatch.domain.vo.DispatchTaskVO;
import com.mes.dispatch.enums.AssignType;
import com.mes.dispatch.enums.AssignmentStatus;
import com.mes.dispatch.enums.DispatchStatus;
import com.mes.dispatch.mapper.DispatchAssignmentMapper;
import com.mes.dispatch.mapper.DispatchTaskMapper;
import com.mes.dispatch.service.IDispatchAssignmentService;
import com.mes.dispatch.service.IDispatchStatusLogService;
import com.mes.dispatch.service.IDispatchTaskService;
import com.mes.framework.security.SecurityUtils;
import com.mes.framework.tenant.TenantContextHolder;
import com.mes.workorder.domain.entity.WorkOrder;
import com.mes.workorder.domain.entity.WorkOrderTask;
import com.mes.workorder.mapper.WorkOrderMapper;
import com.mes.workorder.mapper.WorkOrderTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private final IDispatchStatusLogService statusLogService;
    private final IDispatchAssignmentService assignmentService;
    private final ApplicationEventPublisher eventPublisher;

    /** 允许"修改 / 撤销指派"的状态集合 */
    private static final Set<String> EDITABLE_STATUS = Set.of(
            DispatchStatus.UNASSIGNED.getCode(),
            DispatchStatus.ASSIGNED.getCode()
    );

    /** 允许"再次派工"的状态集合（已完工 / 已撤销 / 开工中 均不可再派工） */
    private static final Set<String> ASSIGNABLE_STATUS = Set.of(
            DispatchStatus.UNASSIGNED.getCode(),
            DispatchStatus.REVOKED.getCode()
    );

    // ==================== 查询（原有逻辑） ====================

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

        String currentUser = currentUsernameOrSystem();
        // P0 修复 R3（mcp30）：显式从 TenantContextHolder 取租户并绑定到每条派工。
        // MetaObjectHandler 在 strictInsertFill 下对已设值字段 no-op，这里显式 set
        // 保证单测不启动 MP 插件链时也能看到正确的 tenantId，形成双保险。
        Long currentTenantId = TenantContextHolder.requireTenantId();
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
            dispatchTask.setCreatedBy(currentUser);
            dispatchTask.setUpdatedBy(currentUser);
            dispatchTask.setCreatedTime(LocalDateTime.now());
            dispatchTask.setUpdatedTime(LocalDateTime.now());
            dispatchTask.setTenantId(currentTenantId);
            save(dispatchTask);
        }

        log.info("从工单 {} 生成 {} 个派工任务", workOrder.getWorkOrderNo(), tasks.size());
    }

    // ==================== P0-03 新增写接口 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DispatchTaskCreateDTO dto) {
        Long currentTenantId = TenantContextHolder.requireTenantId();
        AssertUtil.notNull(dto.getWorkOrderId(), "工单ID不能为空");
        WorkOrder workOrder = workOrderMapper.selectById(dto.getWorkOrderId());
        AssertUtil.notNull(workOrder, "关联工单不存在");
        validatePlanTime(dto.getPlanStartTime(), dto.getPlanEndTime());

        String currentUser = currentUsernameOrSystem();
        // P0 修复 R3（mcp30）：显式注入当前 TenantContext，防止 MetaObjectHandler
        // 因任何原因未生效时派工被写到默认租户 1 上，污染跨租户隔离。
        DispatchTask entity = new DispatchTask();
        entity.setWorkOrderId(dto.getWorkOrderId());
        entity.setWorkOrderTaskId(dto.getWorkOrderTaskId());
        entity.setOrderNo(dto.getOrderNo());
        entity.setProcessNo(dto.getProcessNo());
        entity.setWorkName(dto.getWorkName());
        entity.setPlanWorkCenterId(dto.getPlanWorkCenterId());
        entity.setSerialNo(dto.getSerialNo());
        entity.setProjectName(dto.getProjectName());
        entity.setPlanQty(dto.getPlanQty());
        entity.setQtyUnit(dto.getQtyUnit());
        entity.setPlanStartTime(dto.getPlanStartTime());
        entity.setPlanEndTime(dto.getPlanEndTime());
        entity.setDispatchStatus(DispatchStatus.UNASSIGNED.getCode());
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);
        entity.setCreatedTime(LocalDateTime.now());
        entity.setUpdatedTime(LocalDateTime.now());
        entity.setTenantId(currentTenantId);
        save(entity);

        statusLogService.log(entity.getId(), null,
                DispatchStatus.UNASSIGNED.getCode(),
                "创建派工", "手动创建派工任务");

        log.info("手动创建派工任务: id={}, orderNo={}, processNo={}",
                entity.getId(), entity.getOrderNo(), entity.getProcessNo());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DispatchTaskUpdateDTO dto) {
        DispatchTask entity = getById(dto.getId());
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        // 仅未开工状态可修改基本信息（UNASSIGNED / ASSIGNED）
        AssertUtil.isTrue(EDITABLE_STATUS.contains(entity.getDispatchStatus()),
                "当前状态 [" + entity.getDispatchStatus() + "] 不允许修改，仅未开工的任务可编辑");

        // 计划时间整体校验
        LocalDateTime newStart = dto.getPlanStartTime() != null
                ? dto.getPlanStartTime() : entity.getPlanStartTime();
        LocalDateTime newEnd = dto.getPlanEndTime() != null
                ? dto.getPlanEndTime() : entity.getPlanEndTime();
        validatePlanTime(newStart, newEnd);

        // 只覆盖非 null 字段，保持部分更新语义
        if (StringUtils.hasText(dto.getOrderNo())) entity.setOrderNo(dto.getOrderNo());
        if (StringUtils.hasText(dto.getProcessNo())) entity.setProcessNo(dto.getProcessNo());
        if (StringUtils.hasText(dto.getWorkName())) entity.setWorkName(dto.getWorkName());
        if (dto.getPlanWorkCenterId() != null) entity.setPlanWorkCenterId(dto.getPlanWorkCenterId());
        if (StringUtils.hasText(dto.getSerialNo())) entity.setSerialNo(dto.getSerialNo());
        if (StringUtils.hasText(dto.getProjectName())) entity.setProjectName(dto.getProjectName());
        if (dto.getPlanQty() != null) entity.setPlanQty(dto.getPlanQty());
        if (StringUtils.hasText(dto.getQtyUnit())) entity.setQtyUnit(dto.getQtyUnit());
        if (dto.getPlanStartTime() != null) entity.setPlanStartTime(dto.getPlanStartTime());
        if (dto.getPlanEndTime() != null) entity.setPlanEndTime(dto.getPlanEndTime());

        entity.setUpdatedBy(currentUsernameOrSystem());
        entity.setUpdatedTime(LocalDateTime.now());
        updateById(entity);

        log.info("更新派工任务: id={}", entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, String cancelReason) {
        AssertUtil.isTrue(StringUtils.hasText(cancelReason), "撤销原因不能为空");
        DispatchTask entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        // 已完工的任务不可撤销
        AssertUtil.isFalse(DispatchStatus.COMPLETED.getCode().equals(entity.getDispatchStatus()),
                "已完工的派工任务不可撤销");
        AssertUtil.isFalse(DispatchStatus.CANCELLED.getCode().equals(entity.getDispatchStatus()),
                "派工任务已处于撤销状态，请勿重复操作");

        // 将该任务下所有 ACTIVE 分配全部置为 REVOKED，保持数据一致
        List<DispatchAssignment> activeAssignments = assignmentMapper.selectList(
                new LambdaQueryWrapper<DispatchAssignment>()
                        .eq(DispatchAssignment::getDispatchTaskId, id)
                        .eq(DispatchAssignment::getStatus, AssignmentStatus.ACTIVE.getCode()));
        String currentUser = currentUsernameOrSystem();
        for (DispatchAssignment a : activeAssignments) {
            a.setStatus(AssignmentStatus.REVOKED.getCode());
            a.setRevokedBy(currentUser);
            a.setRevokedTime(LocalDateTime.now());
            assignmentMapper.updateById(a);
        }

        String fromStatus = entity.getDispatchStatus();
        entity.setDispatchStatus(DispatchStatus.CANCELLED.getCode());
        entity.setCancelReason(cancelReason);
        entity.setUpdatedBy(currentUser);
        entity.setUpdatedTime(LocalDateTime.now());
        updateById(entity);

        statusLogService.log(id, fromStatus, DispatchStatus.CANCELLED.getCode(),
                "撤销派工", "撤销原因: " + cancelReason);

        log.info("撤销派工任务: id={}, reason={}", id, cancelReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(DispatchTaskAssignDTO dto) {
        DispatchTask task = getById(dto.getTaskId());
        AssertUtil.notNull(task, "派工任务不存在");

        AssertUtil.isTrue(ASSIGNABLE_STATUS.contains(task.getDispatchStatus()),
                "当前状态 [" + task.getDispatchStatus() + "] 不可派工，仅未分派 / 已撤销指派的任务可派工");

        // 归一化分派类型：EQUIPMENT 作为 DEVICE 的同义词（外部 API 约定）
        String normalizedType = normalizeAssignType(dto.getAssignType());

        // 资源占用冲突校验
        for (int i = 0; i < dto.getAssigneeIds().size(); i++) {
            Long assigneeId = dto.getAssigneeIds().get(i);
            AssertUtil.notNull(assigneeId, "分派对象 ID 不能为空");
            checkAssignmentConflict(task, assigneeId, normalizedType);
        }

        // 逐个落库，复用 IDispatchAssignmentService 的分派逻辑
        List<String> codes = dto.getAssigneeCodes();
        List<String> names = dto.getAssigneeNames();
        for (int i = 0; i < dto.getAssigneeIds().size(); i++) {
            Long assigneeId = dto.getAssigneeIds().get(i);
            DispatchAssignDTO single = new DispatchAssignDTO();
            single.setAssigneeId(assigneeId);
            // 编码为必填字段，若外部未传则兜底为 ID 字符串，保证校验通过
            single.setAssigneeCode(codes != null && i < codes.size() && StringUtils.hasText(codes.get(i))
                    ? codes.get(i) : String.valueOf(assigneeId));
            single.setAssigneeName(names != null && i < names.size() ? names.get(i) : null);
            single.setAssignedQty(dto.getAssignedQty());
            single.setQtyUnit(dto.getQtyUnit());

            switch (normalizedType) {
                case "PERSON" -> assignmentService.assignPerson(task.getId(), single);
                case "DEVICE" -> assignmentService.assignDevice(task.getId(), single);
                case "TEAM" -> assignmentService.assignTeam(task.getId(), single);
                default -> throw new BusinessException("未知分派类型: " + normalizedType);
            }
        }

        log.info("派工指派完成: taskId={}, type={}, count={}",
                task.getId(), normalizedType, dto.getAssigneeIds().size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unassign(Long assignmentId, String reason) {
        AssertUtil.isTrue(StringUtils.hasText(reason), "取消指派原因不能为空");
        // 直接委派给 IDispatchAssignmentService.revoke，复用原有状态回退逻辑
        assignmentService.revoke(assignmentId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void start(Long id) {
        DispatchTask task = getById(id);
        AssertUtil.notNull(task, ResultCode.DATA_NOT_EXIST);

        AssertUtil.isTrue(DispatchStatus.ASSIGNED.getCode().equals(task.getDispatchStatus()),
                "当前状态 [" + task.getDispatchStatus() + "] 不可开工，仅已分派状态可开工");

        String fromStatus = task.getDispatchStatus();
        task.setDispatchStatus(DispatchStatus.IN_PROGRESS.getCode());
        task.setActualStartTime(LocalDateTime.now());
        task.setUpdatedBy(currentUsernameOrSystem());
        task.setUpdatedTime(LocalDateTime.now());
        updateById(task);

        statusLogService.log(id, fromStatus, DispatchStatus.IN_PROGRESS.getCode(),
                "开工", "实际开工时间: " + task.getActualStartTime());

        WorkOrder workOrder = task.getWorkOrderId() == null ? null : workOrderMapper.selectById(task.getWorkOrderId());
        eventPublisher.publishEvent(new DispatchTaskStartedEvent(
                this,
                task.getId(),
                task.getWorkOrderId(),
                task.getWorkOrderTaskId(),
                workOrder != null ? workOrder.getWorkOrderNo() : null,
                task.getProcessNo(),
                task.getWorkName()
        ));

        log.info("派工任务开工: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id, DispatchTaskCompleteDTO dto) {
        DispatchTask task = getById(id);
        AssertUtil.notNull(task, ResultCode.DATA_NOT_EXIST);

        AssertUtil.isTrue(DispatchStatus.IN_PROGRESS.getCode().equals(task.getDispatchStatus()),
                "当前状态 [" + task.getDispatchStatus() + "] 不可完工，仅开工中状态可完工");
        AssertUtil.isTrue(dto.getActualEndTime() != null, "实际完工时间不能为空");

        // 若调用方额外传入实际开工时间则覆盖，保证容错
        if (dto.getActualStartTime() != null) {
            task.setActualStartTime(dto.getActualStartTime());
        }
        AssertUtil.isTrue(task.getActualStartTime() != null
                        && !dto.getActualEndTime().isBefore(task.getActualStartTime()),
                "实际完工时间不能早于实际开工时间");

        String fromStatus = task.getDispatchStatus();
        task.setDispatchStatus(DispatchStatus.COMPLETED.getCode());
        task.setActualEndTime(dto.getActualEndTime());
        task.setActualQty(dto.getActualQty());
        task.setQualityResult(dto.getQualityResult());
        task.setUpdatedBy(currentUsernameOrSystem());
        task.setUpdatedTime(LocalDateTime.now());
        updateById(task);

        String remark = "实际完成数量=" + dto.getActualQty()
                + "；质量结果=" + dto.getQualityResult()
                + (StringUtils.hasText(dto.getRemark()) ? "；备注=" + dto.getRemark() : "");
        statusLogService.log(id, fromStatus, DispatchStatus.COMPLETED.getCode(),
                "完工", remark);

        WorkOrder workOrder = task.getWorkOrderId() == null ? null : workOrderMapper.selectById(task.getWorkOrderId());
        String workOrderNo = workOrder != null ? workOrder.getWorkOrderNo() : null;
        String operator = currentUsernameOrSystem();
        eventPublisher.publishEvent(new DispatchTaskCompletedEvent(
                this,
                task.getId(),
                task.getWorkOrderId(),
                task.getWorkOrderTaskId(),
                workOrderNo,
                task.getProcessNo(),
                task.getWorkName(),
                task.getProjectName(),
                task.getSerialNo(),
                task.getActualQty(),
                task.getQualityResult(),
                task.getActualEndTime(),
                dto.getRemark(),
                operator
        ));

        if ("FAIL".equalsIgnoreCase(task.getQualityResult())) {
            eventPublisher.publishEvent(new DispatchTaskQualityFailedEvent(
                    this,
                    task.getId(),
                    task.getWorkOrderId(),
                    task.getWorkOrderTaskId(),
                    workOrderNo,
                    task.getProcessNo(),
                    task.getWorkName(),
                    task.getProjectName(),
                    task.getSerialNo(),
                    task.getActualQty(),
                    task.getActualEndTime(),
                    dto.getRemark(),
                    operator
            ));
        }

        long unfinishedCount = count(new LambdaQueryWrapper<DispatchTask>()
                .eq(DispatchTask::getWorkOrderId, task.getWorkOrderId())
                .ne(DispatchTask::getDispatchStatus, DispatchStatus.COMPLETED.getCode()));
        if (unfinishedCount == 0) {
            eventPublisher.publishEvent(new DispatchAllTasksCompletedEvent(
                    this, task.getWorkOrderId(), workOrderNo));
        }

        log.info("派工任务完工: id={}, qty={}, quality={}",
                id, dto.getActualQty(), dto.getQualityResult());
    }

    // ==================== 私有辅助 ====================

    /**
     * 资源占用冲突校验：
     * <p>若 assigneeId 在同一 assignType 下已有 ACTIVE 分配，且对应派工任务的计划时间段与本任务重叠，则抛异常。</p>
     * <p>注意：两侧任意一个计划时间为空时，跳过重叠判定（视为不冲突）。</p>
     */
    private void checkAssignmentConflict(DispatchTask currentTask, Long assigneeId, String assignType) {
        List<DispatchAssignment> existing = assignmentMapper.selectList(
                new LambdaQueryWrapper<DispatchAssignment>()
                        .eq(DispatchAssignment::getAssigneeId, assigneeId)
                        .eq(DispatchAssignment::getAssignType, assignType)
                        .eq(DispatchAssignment::getStatus, AssignmentStatus.ACTIVE.getCode()));
        if (existing.isEmpty()) {
            return;
        }

        // 仅当当前任务的计划时间完整时才进行冲突判定
        if (currentTask.getPlanStartTime() == null || currentTask.getPlanEndTime() == null) {
            return;
        }

        List<Long> conflictTaskIds = new ArrayList<>();
        for (DispatchAssignment a : existing) {
            if (a.getDispatchTaskId().equals(currentTask.getId())) {
                // 同一任务内旧记录不算冲突（允许在已撤销后再派工给同一人）
                continue;
            }
            DispatchTask other = getById(a.getDispatchTaskId());
            if (other == null) continue;
            // 已完工 / 已撤销的任务不占用资源
            if (DispatchStatus.COMPLETED.getCode().equals(other.getDispatchStatus())
                    || DispatchStatus.CANCELLED.getCode().equals(other.getDispatchStatus())) {
                continue;
            }
            if (other.getPlanStartTime() == null || other.getPlanEndTime() == null) continue;
            // 重叠判定：[a.start, a.end) 与 [b.start, b.end) 有交集
            boolean overlap = other.getPlanStartTime().isBefore(currentTask.getPlanEndTime())
                    && other.getPlanEndTime().isAfter(currentTask.getPlanStartTime());
            if (overlap) {
                conflictTaskIds.add(other.getId());
            }
        }
        if (!conflictTaskIds.isEmpty()) {
            throw new BusinessException(String.format(
                    "资源占用冲突：%s 对象 [%d] 在时间段 [%s ~ %s] 已被派工任务 %s 占用",
                    AssignType.valueOf(assignType).getDesc(),
                    assigneeId,
                    currentTask.getPlanStartTime(), currentTask.getPlanEndTime(),
                    conflictTaskIds));
        }
    }

    /**
     * 归一化分派类型：EQUIPMENT &rarr; DEVICE（兼容外部约定）
     */
    private String normalizeAssignType(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException("分派类型不能为空");
        }
        String upper = raw.trim().toUpperCase();
        if ("EQUIPMENT".equals(upper)) {
            return AssignType.DEVICE.getCode();
        }
        // 校验是否为合法枚举
        for (AssignType t : AssignType.values()) {
            if (t.getCode().equals(upper)) {
                return t.getCode();
            }
        }
        throw new BusinessException("非法分派类型: " + raw);
    }

    /**
     * 计划时间有效性校验：若两者都提供，则 planStartTime 必须 &lt; planEndTime
     */
    private void validatePlanTime(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null) {
            AssertUtil.isTrue(start.isBefore(end), "计划开始时间必须早于计划结束时间");
        }
    }

    /**
     * 获取当前登录用户名，未登录或匿名时返回 "system"
     */
    private String currentUsernameOrSystem() {
        String username = SecurityUtils.getCurrentUsername();
        return StringUtils.hasText(username) ? username : "system";
    }

    private DispatchTaskVO toVO(DispatchTask entity) {
        DispatchTaskVO vo = new DispatchTaskVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
