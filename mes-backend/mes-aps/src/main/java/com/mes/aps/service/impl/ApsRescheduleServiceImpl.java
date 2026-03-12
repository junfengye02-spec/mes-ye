package com.mes.aps.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.aps.client.ApsClient;
import com.mes.aps.enums.SyncType;
import com.mes.aps.service.IApsRescheduleService;
import com.mes.aps.service.IApsSyncConfigService;
import com.mes.aps.service.IApsUpstreamSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * APS 异常重排触发服务实现
 * <p>包含 5 分钟防抖逻辑</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApsRescheduleServiceImpl implements IApsRescheduleService {

    private static final long DEBOUNCE_INTERVAL_MS = 5 * 60 * 1000L; // 5 分钟

    private final ApsClient apsClient;
    private final IApsSyncConfigService configService;
    private final IApsUpstreamSyncService upstreamSyncService;
    private final ObjectMapper objectMapper;

    /** 按事件分类记录上次触发时间（防抖） */
    private final ConcurrentHashMap<String, Long> lastTriggerTime = new ConcurrentHashMap<>();

    @Override
    public void triggerReschedule(String eventCategory, String reason, Long dataId, String dataNo) {
        if (!isRescheduleEnabled()) {
            log.info("APS 重排触发已关闭，跳过: category={}", eventCategory);
            return;
        }

        // 5 分钟防抖检查
        String category = eventCategory != null ? eventCategory : "DEFAULT";
        long now = System.currentTimeMillis();
        Long lastTime = lastTriggerTime.get(category);

        if (lastTime != null && (now - lastTime) < DEBOUNCE_INTERVAL_MS) {
            log.info("APS 重排防抖: category={}, 距上次触发未满5分钟，写入同步队列待处理", category);
            // 写入队列，定时任务处理时会批量发送
            enqueueReschedule(category, reason, dataId, dataNo);
            return;
        }

        lastTriggerTime.put(category, now);

        // 实时触发重排
        try {
            Map<String, Object> payload = Map.of(
                    "triggerType", "ABNORMAL",
                    "eventCategory", category,
                    "reason", reason != null ? reason : "",
                    "dataId", dataId != null ? dataId : 0,
                    "dataNo", dataNo != null ? dataNo : "",
                    "timestamp", now
            );

            apsClient.post("/api/schedule/combined", payload, Map.class);
            log.info("APS 重排触发成功: category={}, reason={}", category, reason);
        } catch (Exception e) {
            log.error("APS 重排触发失败，写入同步队列: category={}, error={}", category, e.getMessage());
            // 调用失败时写入同步队列，由补偿机制处理
            enqueueReschedule(category, reason, dataId, dataNo);
        }
    }

    @Override
    public boolean isRescheduleEnabled() {
        return configService.getBooleanConfig("aps.sync.reschedule.enabled", true) &&
               configService.getBooleanConfig("aps.sync.enabled", true);
    }

    private void enqueueReschedule(String category, String reason, Long dataId, String dataNo) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "triggerType", "ABNORMAL",
                    "eventCategory", category,
                    "reason", reason != null ? reason : ""
            ));
            upstreamSyncService.enqueue(
                    SyncType.ABNORMAL.getCode(), "RESCHEDULE", dataId, dataNo, 1, payload);
        } catch (Exception e) {
            log.error("写入重排同步队列失败: {}", e.getMessage());
        }
    }
}
