package com.mes.query.listener;

import com.mes.common.event.DispatchTaskCompletedEvent;
import com.mes.common.event.RecheckCompletedEvent;
import com.mes.query.service.IInspectionWorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 检验作业查询投影监听器
 */
@Component
@RequiredArgsConstructor
public class InspectionWorkProjectionListener {

    private final IInspectionWorkService inspectionWorkService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDispatchTaskCompleted(DispatchTaskCompletedEvent event) {
        inspectionWorkService.projectDispatchCompletion(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRecheckCompleted(RecheckCompletedEvent event) {
        inspectionWorkService.projectRecheckCompletion(event);
    }
}
