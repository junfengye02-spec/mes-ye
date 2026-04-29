package com.mes.workorder.search;

import com.mes.framework.es.ElasticsearchConfig.EsIndexNameResolver;
import com.mes.workorder.domain.doc.WorkOrderDoc;
import com.mes.workorder.domain.entity.WorkOrder;
import com.mes.workorder.event.WorkOrderReleasedEvent;
import com.mes.workorder.service.IWorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 工单 ES 异步同步监听器（P2-28）
 *
 * <p>同步策略选型（两种方案详见 elasticsearch-setup.md）：</p>
 * <ul>
 *   <li><b>方案 A（本实现采用）：</b>订阅项目内现有的 Spring ApplicationEvent
 *       （如 {@link WorkOrderReleasedEvent}），在业务事务提交后异步写入 ES。
 *       优点：零外部组件依赖，复用现有事件；<br>
 *       缺点：更新 / 删除路径目前尚未发事件，需业务侧补齐；
 *       首次全量回填需另写一个定时任务。</li>
 *   <li><b>方案 B（备选）：</b>使用 Logstash JDBC input 或 Canal 订阅 MySQL binlog，
 *       优点：对业务代码无侵入，变更捕获完整；<br>
 *       缺点：多一套外部组件，运维成本高，binlog 权限审批流程较长。</li>
 * </ul>
 *
 * <p>本监听器采用方案 A，只处理"工单下发"这个高价值事件作为示例。其余路径（创建 / 更新 / 完工 / 删除）
 * 请按同样模式扩展；全量回填参见 elasticsearch-setup.md 中的 reindex 脚本。</p>
 *
 * <p>激活条件：{@code mes.es.enabled=true}，否则整个 Bean 不注册。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mes.es", name = "enabled", havingValue = "true")
public class WorkOrderEsSyncListener {

    /** 逻辑索引名，与 Document 注解保持一致 */
    private static final String LOGICAL_INDEX = "mes_work_order";

    private final IWorkOrderService workOrderService;
    private final ElasticsearchOperations esOps;
    private final EsIndexNameResolver indexResolver;

    /**
     * 工单下发后异步写入 ES。
     * <ul>
     *   <li>{@link TransactionalEventListener#phase()} 设为 AFTER_COMMIT：业务事务提交后再写 ES，
     *       避免事务回滚后产生脏数据</li>
     *   <li>{@link Async} 使项目已有的 {@code mesDefaultExecutor} 线程池承载写入，
     *       不阻塞业务请求线程</li>
     *   <li>写 ES 失败只 WARN 不抛：ES 是加速层，缺失可通过定时回填任务补偿</li>
     * </ul>
     *
     * @param event 工单下发事件
     */
    @Async("mesDefaultExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkOrderReleased(WorkOrderReleasedEvent event) {
        Long workOrderId = event.getWorkOrderId();
        if (workOrderId == null) {
            return;
        }
        try {
            WorkOrder entity = workOrderService.getById(workOrderId);
            if (entity == null) {
                log.warn("[WorkOrderEsSync] 工单不存在，放弃同步 id={}", workOrderId);
                return;
            }
            upsert(entity);
            log.info("[WorkOrderEsSync] 工单同步 ES 成功 id={}, workOrderNo={}",
                    entity.getId(), entity.getWorkOrderNo());
        } catch (Exception ex) {
            log.warn("[WorkOrderEsSync] 工单同步 ES 失败（忽略）id={}, err={}",
                    workOrderId, ex.getMessage());
        }
    }

    /**
     * 兜底入口：普通 ApplicationEvent 的同步点，业务需要时可直接发非事务事件。
     * 不带事务语义，适用于已经显式落库后的补偿。
     *
     * @param workOrder 工单实体
     */
    @Async("mesDefaultExecutor")
    @EventListener
    public void onManualSync(ManualWorkOrderSyncEvent event) {
        try {
            upsert(event.getWorkOrder());
        } catch (Exception ex) {
            log.warn("[WorkOrderEsSync] 手动同步失败 id={}, err={}",
                    event.getWorkOrder() == null ? null : event.getWorkOrder().getId(),
                    ex.getMessage());
        }
    }

    /**
     * Upsert 到按租户分片的索引。
     *
     * @param entity 工单实体
     */
    private void upsert(WorkOrder entity) {
        WorkOrderDoc doc = toDoc(entity);
        String indexName = indexResolver.resolve(LOGICAL_INDEX, entity.getTenantId());
        esOps.save(doc, IndexCoordinates.of(indexName));
    }

    /**
     * 实体 → 文档转换，仅保留 ES 需要的字段。
     *
     * @param entity 工单实体
     * @return 文档对象
     */
    private WorkOrderDoc toDoc(WorkOrder entity) {
        WorkOrderDoc doc = new WorkOrderDoc();
        BeanUtils.copyProperties(entity, doc);
        return doc;
    }

    /**
     * 手动同步事件：业务侧在非"下发"路径也需要写 ES 时 publish 它。
     * 独立为 Pojo，避免把 Repository 调用直接写在 Service 里。
     */
    public static class ManualWorkOrderSyncEvent {
        private final WorkOrder workOrder;

        public ManualWorkOrderSyncEvent(WorkOrder workOrder) {
            this.workOrder = workOrder;
        }

        public WorkOrder getWorkOrder() {
            return workOrder;
        }
    }
}
