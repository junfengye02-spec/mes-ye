package com.mes.quality.listener;

import com.mes.common.event.AbnormalSubmittedEvent;
import com.mes.common.event.DispatchTaskQualityFailedEvent;
import com.mes.common.event.DispatchTaskStartedEvent;
import com.mes.quality.domain.dto.RecheckRequestDTO;
import com.mes.quality.domain.dto.WorkStartCheckDTO;
import com.mes.quality.service.IRecheckRequestService;
import com.mes.quality.service.IWorkStartCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 派工/异常事件驱动的质量联动监听器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QualityEventListener {

    private final IRecheckRequestService recheckRequestService;
    private final IWorkStartCheckService workStartCheckService;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onDispatchTaskStarted(DispatchTaskStartedEvent event) {
        WorkStartCheckDTO dto = new WorkStartCheckDTO();
        dto.setWorkNo(event.getWorkNo());
        dto.setWorkOrderTaskId(event.getWorkOrderTaskId());
        dto.setWorkOrderId(event.getWorkOrderId());
        dto.setWorkOrderNo(event.getWorkOrderNo());
        dto.setCheckItem("派工开工检查");
        dto.setCheckResult("AUTO_CREATED");
        dto.setCheckStatus("PENDING");
        dto.setCheckRemark("派工任务开工时系统自动创建");
        dto.setRemark(event.getWorkName());
        workStartCheckService.create(dto);
    }

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onDispatchTaskQualityFailed(DispatchTaskQualityFailedEvent event) {
        RecheckRequestDTO dto = new RecheckRequestDTO();
        dto.setWorkOrderId(event.getWorkOrderId());
        dto.setDispatchTaskId(event.getDispatchTaskId());
        dto.setProjectName(event.getProjectName());
        dto.setMaterialCode(event.getSerialNo());
        dto.setMaterialName(event.getWorkName());
        dto.setProductionOrderNo(event.getWorkOrderNo());
        dto.setRecheckRequirement("派工任务完工质量结果为 FAIL，需发起复检");
        dto.setRecheckReason(buildFailureReason(event.getWorkNo(), event.getRemark()));
        dto.setRecheckProposer(event.getOperator());
        dto.setRecheckProposeTime(event.getActualEndTime());
        recheckRequestService.create(dto);
    }

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onAbnormalSubmitted(AbnormalSubmittedEvent event) {
        if (event.getWorkOrderId() == null && event.getDispatchTaskId() == null
                && !StringUtils.hasText(event.getOrderNo())) {
            return;
        }

        RecheckRequestDTO dto = new RecheckRequestDTO();
        dto.setWorkOrderId(event.getWorkOrderId());
        dto.setDispatchTaskId(event.getDispatchTaskId());
        dto.setProductionOrderNo(event.getOrderNo());
        dto.setRecheckRequirement("异常联络单提交后触发质量复检");
        dto.setRecheckReason("异常联络单 " + event.getContactNo()
                + " 已提交；分类=" + event.getEventCategory()
                + (StringUtils.hasText(event.getAbnormalDesc()) ? "；描述=" + event.getAbnormalDesc() : ""));
        dto.setRecheckProposer("system");
        dto.setRecheckProposeTime(LocalDateTime.now());
        recheckRequestService.create(dto);
    }

    private String buildFailureReason(String workNo, String remark) {
        StringBuilder builder = new StringBuilder("派工任务 ");
        if (StringUtils.hasText(workNo)) {
            builder.append(workNo).append(' ');
        }
        builder.append("完工质量结果为 FAIL");
        if (StringUtils.hasText(remark)) {
            builder.append("；备注=").append(remark);
        }
        return builder.toString();
    }
}
