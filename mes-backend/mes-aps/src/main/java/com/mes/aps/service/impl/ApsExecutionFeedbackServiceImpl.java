package com.mes.aps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.aps.domain.entity.ApsSyncLog;
import com.mes.aps.enums.ApsExecutionFeedbackType;
import com.mes.aps.enums.SyncDirection;
import com.mes.aps.service.IApsExecutionFeedbackService;
import com.mes.aps.service.IApsSyncLogService;
import com.mes.aps.service.IApsUpstreamSyncService;
import com.mes.dispatch.domain.entity.DispatchAssignment;
import com.mes.dispatch.domain.entity.DispatchTask;
import com.mes.dispatch.mapper.DispatchAssignmentMapper;
import com.mes.dispatch.mapper.DispatchTaskMapper;
import com.mes.material.domain.entity.MaterialRequisition;
import com.mes.material.mapper.MaterialRequisitionMapper;
import com.mes.quality.domain.entity.ShiftHandover;
import com.mes.quality.mapper.ShiftHandoverMapper;
import com.mes.workorder.domain.entity.*;
import com.mes.workorder.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApsExecutionFeedbackServiceImpl implements IApsExecutionFeedbackService {

    private final IApsUpstreamSyncService upstreamSyncService;
    private final IApsSyncLogService syncLogService;
    private final ObjectMapper objectMapper;
    private final DispatchTaskMapper dispatchTaskMapper;
    private final DispatchAssignmentMapper dispatchAssignmentMapper;
    private final ShiftHandoverMapper shiftHandoverMapper;
    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderInputMaterialMapper inputMaterialMapper;
    private final WorkOrderConstraintMapper constraintMapper;
    private final WorkOrderSupplyPlanMapper supplyPlanMapper;
    private final MaterialRequisitionMapper requisitionMapper;

    @Override
    public void feedbackDispatchAssignment(Long dispatchTaskId, Long assignmentId) {
        try {
            DispatchTask task = dispatchTaskMapper.selectById(dispatchTaskId);
            DispatchAssignment assignment = dispatchAssignmentMapper.selectById(assignmentId);
            if (task == null || assignment == null) return;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("workOrderId", task.getWorkOrderId());
            payload.put("workOrderTaskId", task.getWorkOrderTaskId());
            payload.put("orderNo", task.getOrderNo());
            payload.put("processNo", task.getProcessNo());
            payload.put("planWorkCenterId", task.getPlanWorkCenterId());
            payload.put("assignType", assignment.getAssignType());
            payload.put("assigneeCode", assignment.getAssigneeCode());
            payload.put("assigneeName", assignment.getAssigneeName());
            payload.put("assignedQty", assignment.getAssignedQty());
            payload.put("status", assignment.getStatus());
            payload.put("assignedTime", assignment.getAssignedTime());

            enqueue(ApsExecutionFeedbackType.DISPATCH, "DISPATCH_ASSIGNMENT", assignmentId,
                    task.getOrderNo(), 3, payload);
        } catch (Exception e) {
            log.error("派工分配反馈失败: dispatchTaskId={}, error={}", dispatchTaskId, e.getMessage());
        }
    }

    @Override
    public void feedbackStartCheck(Long workOrderTaskId, String checkStatus) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("workOrderTaskId", workOrderTaskId);
            payload.put("checkStatus", checkStatus);
            payload.put("checkTime", System.currentTimeMillis());

            enqueue(ApsExecutionFeedbackType.START_CHECK, "WORK_START_CHECK", workOrderTaskId,
                    null, "FAILED".equals(checkStatus) ? 2 : 5, payload);
        } catch (Exception e) {
            log.error("开工检查反馈失败: workOrderTaskId={}, error={}", workOrderTaskId, e.getMessage());
        }
    }

    @Override
    public void feedbackWorkOrderConstraint(Long workOrderId) {
        try {
            List<WorkOrderConstraint> constraints = constraintMapper.selectList(
                    new LambdaQueryWrapper<WorkOrderConstraint>()
                            .eq(WorkOrderConstraint::getWorkOrderId, workOrderId));

            List<Map<String, Object>> constraintList = constraints.stream().map(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("constraintType", c.getConstraintType());
                m.put("relatedWorkOrderId", c.getRelatedWorkOrderId());
                m.put("relatedTaskId", c.getRelatedTaskId());
                m.put("remark", c.getRemark());
                return m;
            }).toList();

            WorkOrder wo = workOrderMapper.selectById(workOrderId);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("workOrderId", workOrderId);
            payload.put("workOrderNo", wo != null ? wo.getWorkOrderNo() : null);
            payload.put("constraints", constraintList);

            enqueue(ApsExecutionFeedbackType.CONSTRAINT, "WORK_ORDER_CONSTRAINT", workOrderId,
                    wo != null ? wo.getWorkOrderNo() : null, 4, payload);
        } catch (Exception e) {
            log.error("工单约束反馈失败: workOrderId={}, error={}", workOrderId, e.getMessage());
        }
    }

    @Override
    public void feedbackShiftOutput(Long shiftHandoverId) {
        try {
            ShiftHandover handover = shiftHandoverMapper.selectById(shiftHandoverId);
            if (handover == null) return;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("projectName", handover.getProjectName());
            payload.put("productSerialNo", handover.getProductSerialNo());
            payload.put("processContent", handover.getProcessContent());
            payload.put("handoverDate", handover.getHandoverDate());
            payload.put("handoverShift", handover.getHandoverShift());
            payload.put("handoverTeamName", handover.getHandoverTeamName());
            payload.put("planQty", handover.getPlanQty());
            payload.put("actualQty", handover.getActualQty());
            payload.put("gapAnalysis", handover.getGapAnalysis());

            enqueue(ApsExecutionFeedbackType.SHIFT_OUTPUT, "SHIFT_HANDOVER", shiftHandoverId,
                    handover.getProductSerialNo(), 3, payload);
        } catch (Exception e) {
            log.error("交班产出反馈失败: shiftHandoverId={}, error={}", shiftHandoverId, e.getMessage());
        }
    }

    @Override
    public void feedbackMaterialShortage(Long workOrderId) {
        try {
            WorkOrder wo = workOrderMapper.selectById(workOrderId);
            if (wo == null) return;

            List<WorkOrderInputMaterial> materials = inputMaterialMapper.selectList(
                    new LambdaQueryWrapper<WorkOrderInputMaterial>()
                            .eq(WorkOrderInputMaterial::getWorkOrderId, workOrderId));

            List<Map<String, Object>> shortageList = new ArrayList<>();
            for (WorkOrderInputMaterial m : materials) {
                var issued = m.getIssuedQty() != null ? m.getIssuedQty() : java.math.BigDecimal.ZERO;
                var required = m.getRequiredQty() != null ? m.getRequiredQty() : java.math.BigDecimal.ZERO;
                if (issued.compareTo(required) < 0) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("materialCode", m.getMaterialCode());
                    item.put("materialName", m.getMaterialName());
                    item.put("requiredQty", required);
                    item.put("issuedQty", issued);
                    item.put("shortageQty", required.subtract(issued));
                    shortageList.add(item);
                }
            }

            if (shortageList.isEmpty()) return;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("workOrderId", workOrderId);
            payload.put("workOrderNo", wo.getWorkOrderNo());
            payload.put("shortageItems", shortageList);

            enqueue(ApsExecutionFeedbackType.MATERIAL_SHORTAGE, "MATERIAL_SHORTAGE", workOrderId,
                    wo.getWorkOrderNo(), 2, payload);
        } catch (Exception e) {
            log.error("物料短缺反馈失败: workOrderId={}, error={}", workOrderId, e.getMessage());
        }
    }

    @Override
    public void feedbackRequisitionProgress(Long requisitionId) {
        try {
            MaterialRequisition req = requisitionMapper.selectById(requisitionId);
            if (req == null) return;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("requisitionNo", req.getRequisitionNo());
            payload.put("workOrderNo", req.getWorkOrderNo());
            payload.put("productCode", req.getProductCode());
            payload.put("planQty", req.getPlanQty());
            payload.put("actualQty", req.getActualQty());
            payload.put("qualifiedQty", req.getQualifiedQty());
            payload.put("status", req.getStatus());

            enqueue(ApsExecutionFeedbackType.REQUISITION, "MATERIAL_REQUISITION", requisitionId,
                    req.getRequisitionNo(), 4, payload);
        } catch (Exception e) {
            log.error("领料进度反馈失败: requisitionId={}, error={}", requisitionId, e.getMessage());
        }
    }

    @Override
    public void feedbackSupplyProgress(Long workOrderId, Long supplyPlanId) {
        try {
            WorkOrderSupplyPlan plan = supplyPlanMapper.selectById(supplyPlanId);
            WorkOrder wo = workOrderMapper.selectById(workOrderId);
            if (plan == null) return;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("workOrderId", workOrderId);
            payload.put("workOrderNo", wo != null ? wo.getWorkOrderNo() : null);
            payload.put("demandPlanNo", plan.getDemandPlanNo());
            payload.put("supplyPlanNo", plan.getSupplyPlanNo());
            payload.put("supplyQty", plan.getSupplyQty());
            payload.put("completedQty", plan.getCompletedQty());

            enqueue(ApsExecutionFeedbackType.SUPPLY_PROGRESS, "SUPPLY_PLAN", supplyPlanId,
                    plan.getSupplyPlanNo(), 4, payload);
        } catch (Exception e) {
            log.error("供应计划反馈失败: supplyPlanId={}, error={}", supplyPlanId, e.getMessage());
        }
    }

    @Override
    public void feedbackWorkOrderStatusChange(Long workOrderId, String oldStatus, String newStatus) {
        try {
            WorkOrder wo = workOrderMapper.selectById(workOrderId);
            if (wo == null) return;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("workOrderId", workOrderId);
            payload.put("workOrderNo", wo.getWorkOrderNo());
            payload.put("orderPlanNo", wo.getOrderPlanNo());
            payload.put("productCode", wo.getProductCode());
            payload.put("oldStatus", oldStatus);
            payload.put("newStatus", newStatus);
            payload.put("planStartTime", wo.getPlanStartTime());
            payload.put("planEndTime", wo.getPlanEndTime());
            payload.put("actualStartTime", wo.getActualStartTime());
            payload.put("actualEndTime", wo.getActualEndTime());
            payload.put("changeTime", System.currentTimeMillis());

            enqueue(ApsExecutionFeedbackType.STATUS_CHANGE, "WORK_ORDER", workOrderId,
                    wo.getWorkOrderNo(), 2, payload);
        } catch (Exception e) {
            log.error("工单状态变更反馈失败: workOrderId={}, error={}", workOrderId, e.getMessage());
        }
    }

    @Override
    public void feedbackProcessChange(String changeType, Long entityId, String entityCode) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("changeType", changeType);
            payload.put("entityId", entityId);
            payload.put("entityCode", entityCode);
            payload.put("changeTime", System.currentTimeMillis());

            enqueue(ApsExecutionFeedbackType.PROCESS_CHANGE, changeType, entityId,
                    entityCode, 3, payload);
        } catch (Exception e) {
            log.error("工艺变更反馈失败: changeType={}, entityId={}, error={}", changeType, entityId, e.getMessage());
        }
    }

    private void enqueue(ApsExecutionFeedbackType feedbackType, String dataType, Long dataId,
                         String dataNo, int priority, Map<String, Object> payload) {
        recordUnsupportedFeedback(feedbackType, dataType, dataId, dataNo);
    }

    private void recordUnsupportedFeedback(ApsExecutionFeedbackType syncType, String dataType, Long dataId, String dataNo) {
        String batchId = UUID.randomUUID().toString();
        String errorMessage = String.format(
                "APS 当前合同不支持该执行反馈类型: %s (dataType=%s, dataId=%s, dataNo=%s)",
                syncType.getCode(), dataType, dataId, dataNo);
        try {
            ApsSyncLog syncLog = syncLogService.createLog(
                    batchId,
                    SyncDirection.UPSTREAM.getCode(),
                    syncType.getCode());
            if (syncLog != null && syncLog.getId() != null) {
                syncLogService.completeLog(syncLog.getId(), 0, 0, 1, errorMessage);
            }
            log.warn("APS 执行反馈未入队，已记本地失败审计: {}", errorMessage);
        } catch (Exception e) {
            log.error("记录APS执行反馈失败审计失败: syncType={}, dataType={}, dataId={}, error={}",
                    syncType.getCode(), dataType, dataId, e.getMessage(), e);
        }
    }
}
