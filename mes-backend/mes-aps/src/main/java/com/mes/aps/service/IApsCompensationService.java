package com.mes.aps.service;

import com.mes.aps.domain.vo.ApsSyncResultVO;

/**
 * APS 补偿同步服务
 * <p>APS 恢复可用后，处理积压的待同步数据</p>
 */
public interface IApsCompensationService {

    /** 执行补偿同步 */
    ApsSyncResultVO compensate();

    /** 获取待补偿数量 */
    long getPendingCount();
}
