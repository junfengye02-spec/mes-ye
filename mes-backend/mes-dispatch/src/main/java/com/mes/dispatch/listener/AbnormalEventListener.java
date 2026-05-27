package com.mes.dispatch.listener;

import com.mes.common.event.AbnormalSubmittedEvent;
import com.mes.dispatch.domain.entity.DispatchTask;
import com.mes.dispatch.enums.DispatchStatus;
import com.mes.dispatch.mapper.DispatchTaskMapper;
import com.mes.dispatch.service.IDispatchStatusLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static com.mes.dispatch.enums.DispatchStatus.ABNORMAL;
import static com.mes.dispatch.enums.DispatchStatus.CANCELLED;
import static com.mes.dispatch.enums.DispatchStatus.COMPLETED;

/**
 * 异常联络单驱动的派工状态联动
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AbnormalEventListener {

    private final DispatchTaskMapper dispatchTaskMapper;
    private final IDispatchStatusLogService statusLogService;

    @EventListener
    public void onAbnormalSubmitted(AbnormalSubmittedEvent event) {
        if (event.getDispatchTaskId() == null) {
            return;
        }

        DispatchTask task = dispatchTaskMapper.selectById(event.getDispatchTaskId());
        if (task == null) {
            log.warn("异常联络单关联派工任务不存在, dispatchTaskId={}", event.getDispatchTaskId());
            return;
        }

        String currentStatus = task.getDispatchStatus();
        if (COMPLETED.getCode().equals(currentStatus)
                || CANCELLED.getCode().equals(currentStatus)
                || ABNORMAL.getCode().equals(currentStatus)) {
            return;
        }

        task.setDispatchStatus(ABNORMAL.getCode());
        dispatchTaskMapper.updateById(task);
        statusLogService.log(task.getId(), currentStatus, ABNORMAL.getCode(), "异常提报",
                "异常联络单 " + event.getContactNo() + " 已提交");
        log.info("异常联络单驱动派工任务标记异常: dispatchTaskId={}, contactNo={}",
                task.getId(), event.getContactNo());
    }
}
