package com.mes.aps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.aps.domain.entity.ApsSyncLog;
import com.mes.aps.domain.query.ApsSyncLogQuery;
import com.mes.aps.domain.vo.ApsSyncLogVO;
import com.mes.aps.enums.SyncStatus;
import com.mes.aps.mapper.ApsSyncLogMapper;
import com.mes.aps.service.IApsSyncLogService;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApsSyncLogServiceImpl extends ServiceImpl<ApsSyncLogMapper, ApsSyncLog>
        implements IApsSyncLogService {

    @Override
    public PageResult<ApsSyncLogVO> page(ApsSyncLogQuery query) {
        LambdaQueryWrapper<ApsSyncLog> wrapper = new LambdaQueryWrapper<ApsSyncLog>()
                .eq(StringUtils.hasText(query.getSyncDirection()),
                        ApsSyncLog::getSyncDirection, query.getSyncDirection())
                .eq(StringUtils.hasText(query.getSyncType()),
                        ApsSyncLog::getSyncType, query.getSyncType())
                .eq(StringUtils.hasText(query.getStatus()),
                        ApsSyncLog::getStatus, query.getStatus())
                .ge(query.getStartTimeFrom() != null,
                        ApsSyncLog::getStartTime, query.getStartTimeFrom())
                .le(query.getStartTimeTo() != null,
                        ApsSyncLog::getStartTime, query.getStartTimeTo())
                .orderByDesc(ApsSyncLog::getCreatedTime);

        Page<ApsSyncLog> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        List<ApsSyncLogVO> voList = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public ApsSyncLogVO getDetail(Long id) {
        ApsSyncLog entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApsSyncLog createLog(String batchId, String direction, String type) {
        ApsSyncLog logEntity = new ApsSyncLog();
        logEntity.setBatchId(batchId);
        logEntity.setSyncDirection(direction);
        logEntity.setSyncType(type);
        logEntity.setStatus(SyncStatus.PROCESSING.getCode());
        logEntity.setStartTime(LocalDateTime.now());
        logEntity.setCreatedTime(LocalDateTime.now());
        logEntity.setTotalCount(0);
        logEntity.setSuccessCount(0);
        logEntity.setFailCount(0);
        save(logEntity);
        return logEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeLog(Long logId, int totalCount, int successCount, int failCount, String errorMessage) {
        ApsSyncLog logEntity = getById(logId);
        if (logEntity == null) return;

        logEntity.setTotalCount(totalCount);
        logEntity.setSuccessCount(successCount);
        logEntity.setFailCount(failCount);
        logEntity.setEndTime(LocalDateTime.now());
        logEntity.setErrorMessage(errorMessage);

        if (logEntity.getStartTime() != null) {
            long duration = java.time.Duration.between(logEntity.getStartTime(), logEntity.getEndTime()).toMillis();
            logEntity.setDurationMs(duration);
        }

        if (failCount == 0) {
            logEntity.setStatus(SyncStatus.SUCCESS.getCode());
        } else if (successCount == 0) {
            logEntity.setStatus(SyncStatus.FAIL.getCode());
        } else {
            logEntity.setStatus(SyncStatus.PARTIAL.getCode());
        }

        updateById(logEntity);
    }

    private ApsSyncLogVO toVO(ApsSyncLog entity) {
        ApsSyncLogVO vo = new ApsSyncLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
