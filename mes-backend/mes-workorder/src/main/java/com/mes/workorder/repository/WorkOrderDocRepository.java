package com.mes.workorder.repository;

import com.mes.workorder.domain.doc.WorkOrderDoc;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 工单 ES Repository（P2-28）
 *
 * <p>仅在 {@code mes.es.enabled=true} 时才由 Spring Data 生成实现类；关闭时整个 Bean 不注册，
 * 保证 ES 未启用的环境启动不受影响。</p>
 *
 * <p>继承自 {@link ElasticsearchRepository} 已提供：</p>
 * <ul>
 *   <li>save / saveAll：upsert 文档</li>
 *   <li>findById：按 _id 查</li>
 *   <li>search(Query)：接入 NativeQuery 做复杂检索</li>
 * </ul>
 *
 * <p>注意：按租户分索引的场景下，业务层必须用 {@code ElasticsearchOperations} 显式传
 * {@code IndexCoordinates.of(indexName)}，不要只靠 Repository 的默认索引。</p>
 */
@Repository
@ConditionalOnProperty(prefix = "mes.es", name = "enabled", havingValue = "true")
public interface WorkOrderDocRepository extends ElasticsearchRepository<WorkOrderDoc, Long> {
}
