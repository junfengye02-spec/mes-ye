package com.mes.aps.listener;

import com.mes.aps.enums.ApsExecutionFeedbackType;
import com.mes.aps.service.IApsExecutionFeedbackService;
import com.mes.common.event.ApsSyncEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * APS 执行反馈事件监听器
 * <p>监听业务模块发布的扩展同步事件，自动触发对应的反馈处理</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApsExecutionFeedbackListener {

    private final IApsExecutionFeedbackService feedbackService;

    @Async
    @EventListener
    public void handleExecutionFeedback(ApsSyncEvent event) {
        ApsExecutionFeedbackType syncType = ApsExecutionFeedbackType.fromCode(event.getSyncType())
                .orElse(null);
        if (syncType == null) {
            return;
        }

        switch (syncType) {
            case DISPATCH -> {
                log.debug("收到派工反馈事件: dataNo={}", event.getDataNo());
                feedbackService.feedbackDispatchAssignment(event.getDataId(),
                        parseLongFromPayload(event.getPayload(), "assignmentId"));
            }
            case START_CHECK -> {
                log.debug("收到开工检查反馈事件: dataNo={}", event.getDataNo());
                feedbackService.feedbackStartCheck(event.getDataId(), event.getDataType());
            }
            case CONSTRAINT -> {
                log.debug("收到工单约束反馈事件: dataNo={}", event.getDataNo());
                feedbackService.feedbackWorkOrderConstraint(event.getDataId());
            }
            case SHIFT_OUTPUT -> {
                log.debug("收到交班产出反馈事件: dataNo={}", event.getDataNo());
                feedbackService.feedbackShiftOutput(event.getDataId());
            }
            case MATERIAL_SHORTAGE -> {
                log.debug("收到物料短缺反馈事件: dataNo={}", event.getDataNo());
                feedbackService.feedbackMaterialShortage(event.getDataId());
            }
            case REQUISITION -> {
                log.debug("收到领料进度反馈事件: dataNo={}", event.getDataNo());
                feedbackService.feedbackRequisitionProgress(event.getDataId());
            }
            case SUPPLY_PROGRESS -> {
                log.debug("收到供应计划反馈事件: dataNo={}", event.getDataNo());
                feedbackService.feedbackSupplyProgress(event.getDataId(),
                        parseLongFromPayload(event.getPayload(), "supplyPlanId"));
            }
            case STATUS_CHANGE -> {
                log.debug("收到工单状态变更事件: dataNo={}", event.getDataNo());
                feedbackService.feedbackWorkOrderStatusChange(event.getDataId(),
                        extractField(event.getPayload(), "oldStatus"),
                        extractField(event.getPayload(), "newStatus"));
            }
            case PROCESS_CHANGE -> {
                log.debug("收到工艺变更事件: dataNo={}", event.getDataNo());
                feedbackService.feedbackProcessChange(event.getDataType(),
                        event.getDataId(), event.getDataNo());
            }
            default -> {
                // 已通过 ApsExecutionFeedbackType 过滤，这里仅为 switch 完整性保留
            }
        }
    }

    private Long parseLongFromPayload(String payload, String key) {
        if (payload == null) return null;
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(payload);
            return node.has(key) ? node.get(key).asLong() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractField(String payload, String key) {
        if (payload == null) return null;
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(payload);
            return node.has(key) ? node.get(key).asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
