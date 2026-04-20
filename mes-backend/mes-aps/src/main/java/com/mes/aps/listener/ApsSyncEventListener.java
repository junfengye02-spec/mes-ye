package com.mes.aps.listener;

import com.mes.aps.service.IApsRescheduleService;
import com.mes.aps.service.IApsUpstreamSyncService;
import com.mes.common.event.ApsSyncEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * APS 同步事件监听器
 * <p>监听业务模块发布的 ApsSyncEvent，写入同步队列</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApsSyncEventListener {

    private final IApsUpstreamSyncService upstreamSyncService;
    private final IApsRescheduleService rescheduleService;

    private static final java.util.Set<String> EXECUTION_FEEDBACK_TYPES = java.util.Set.of(
            "DISPATCH", "START_CHECK", "CONSTRAINT", "SHIFT_OUTPUT",
            "MATERIAL_SHORTAGE", "REQUISITION", "SUPPLY_PROGRESS",
            "STATUS_CHANGE", "PROCESS_CHANGE"
    );

    @Async
    @EventListener
    public void handleApsSyncEvent(ApsSyncEvent event) {
        log.debug("收到 APS 同步事件: type={}, dataNo={}", event.getSyncType(), event.getDataNo());

        // 执行反馈类型由 ApsExecutionFeedbackListener 处理
        if (EXECUTION_FEEDBACK_TYPES.contains(event.getSyncType())) {
            return;
        }

        try {
            if ("ABNORMAL".equals(event.getSyncType())) {
                rescheduleService.triggerReschedule(
                        event.getDataType(), event.getPayload(),
                        event.getDataId(), event.getDataNo());
            } else {
                upstreamSyncService.enqueue(
                        event.getSyncType(), event.getDataType(),
                        event.getDataId(), event.getDataNo(),
                        event.getPriority(), event.getPayload());
            }
        } catch (Exception e) {
            log.error("处理 APS 同步事件失败: type={}, error={}", event.getSyncType(), e.getMessage(), e);
        }
    }
}
