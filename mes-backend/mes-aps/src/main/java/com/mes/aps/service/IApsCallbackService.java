package com.mes.aps.service;

import com.mes.aps.domain.vo.ApsScheduleCallbackVO;

/**
 * APS 回调处理服务
 */
public interface IApsCallbackService {

    /**
     * 处理排程结果回调（SUCCESS / FAILED）
     */
    void handleScheduleResult(ApsScheduleCallbackVO callback);

    /**
     * 处理请求被拒绝回调（REJECTED）
     */
    void handleRequestRejected(ApsScheduleCallbackVO callback);
}
