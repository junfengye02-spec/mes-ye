package com.mes.aps.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.aps.domain.entity.ApsSyncLog;
import com.mes.aps.domain.query.ApsSyncLogQuery;
import com.mes.aps.domain.vo.ApsSyncLogVO;
import com.mes.common.core.PageResult;

public interface IApsSyncLogService extends IService<ApsSyncLog> {
    PageResult<ApsSyncLogVO> page(ApsSyncLogQuery query);
    ApsSyncLogVO getDetail(Long id);

    /**
     * 创建同步日志（由同步服务内部调用）
     */
    ApsSyncLog createLog(String batchId, String direction, String type);

    /**
     * 完成同步日志
     */
    void completeLog(Long logId, int totalCount, int successCount, int failCount, String errorMessage);
}
