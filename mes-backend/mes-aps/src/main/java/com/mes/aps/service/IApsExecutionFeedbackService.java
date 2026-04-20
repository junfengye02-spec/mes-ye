package com.mes.aps.service;

/**
 * APS 执行反馈服务
 * <p>负责将 MES 车间执行数据封装为 APS 可消费的 JSON 并写入同步队列</p>
 */
public interface IApsExecutionFeedbackService {

    void feedbackDispatchAssignment(Long dispatchTaskId, Long assignmentId);

    void feedbackStartCheck(Long workOrderTaskId, String checkStatus);

    void feedbackWorkOrderConstraint(Long workOrderId);

    void feedbackShiftOutput(Long shiftHandoverId);

    void feedbackMaterialShortage(Long workOrderId);

    void feedbackRequisitionProgress(Long requisitionId);

    void feedbackSupplyProgress(Long workOrderId, Long supplyPlanId);

    void feedbackWorkOrderStatusChange(Long workOrderId, String oldStatus, String newStatus);

    void feedbackProcessChange(String changeType, Long entityId, String entityCode);
}
