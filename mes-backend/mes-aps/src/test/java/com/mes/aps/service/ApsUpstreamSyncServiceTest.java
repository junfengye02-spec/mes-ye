package com.mes.aps.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.aps.client.ApsClient;
import com.mes.aps.domain.entity.ApsSyncLog;
import com.mes.aps.domain.entity.ApsSyncQueue;
import com.mes.aps.domain.vo.ApsSyncResultVO;
import com.mes.aps.enums.SyncDirection;
import com.mes.aps.enums.SyncStatus;
import com.mes.aps.mapper.ApsSyncQueueMapper;
import com.mes.aps.service.impl.ApsUpstreamSyncServiceImpl;
import com.mes.plan.mapper.OrderPlanMapper;
import com.mes.workorder.mapper.WorkOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ApsUpstreamSyncServiceImpl} 单元测试。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApsUpstreamSyncServiceTest {

    @Mock
    private ApsSyncQueueMapper syncQueueMapper;
    @Mock
    private ApsClient apsClient;
    @Mock
    private IApsSyncConfigService configService;
    @Mock
    private IApsSyncLogService syncLogService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private OrderPlanMapper orderPlanMapper;
    @Mock
    private WorkOrderMapper workOrderMapper;

    private ApsUpstreamSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApsUpstreamSyncServiceImpl(syncQueueMapper, apsClient, configService,
                syncLogService, objectMapper, orderPlanMapper, workOrderMapper);
    }

    @Test
    @DisplayName("处理队列 - APS不支持的同步类型直接终态失败且不调用HTTP")
    void processQueue_marksUnsupportedTypeFailedWithoutRetryOrHttp() {
        ApsSyncQueue item = new ApsSyncQueue();
        item.setId(1L);
        item.setSyncType("DISPATCH");
        item.setDataType("DISPATCH");
        item.setDataId(10L);
        item.setDataNo("DT-1");
        item.setRetryCount(0);
        item.setMaxRetry(3);
        item.setSyncStatus(SyncStatus.PENDING.getCode());

        when(syncLogService.createLog(anyString(), eq(SyncDirection.UPSTREAM.getCode()), eq("QUEUE")))
                .thenReturn(syncLog(99L));
        when(configService.getBooleanConfig("aps.sync.upstream.enabled", true)).thenReturn(true);
        when(configService.getIntConfig("aps.sync.batch.size", 200)).thenReturn(200);
        when(syncQueueMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

        ApsSyncResultVO result = service.processQueue();

        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        verify(apsClient, never()).post(anyString(), any(), any());
        verify(apsClient, never()).postAsync(anyString(), any());
        verify(syncQueueMapper).updateById(argThat(q ->
                SyncStatus.FAILED.getCode().equals(q.getSyncStatus())
                        && q.getRetryCount() == 0
                        && q.getErrorMessage().contains("APS不支持")));
    }

    private static ApsSyncLog syncLog(Long id) {
        ApsSyncLog log = new ApsSyncLog();
        log.setId(id);
        log.setBatchId("batch-1");
        log.setSyncDirection(SyncDirection.UPSTREAM.getCode());
        log.setSyncType("QUEUE");
        return log;
    }
}
