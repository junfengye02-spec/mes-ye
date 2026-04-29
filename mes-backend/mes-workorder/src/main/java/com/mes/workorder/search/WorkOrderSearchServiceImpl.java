package com.mes.workorder.search;

import com.mes.common.core.PageResult;
import com.mes.workorder.domain.doc.WorkOrderDoc;
import com.mes.workorder.domain.query.WorkOrderQuery;
import com.mes.workorder.domain.vo.WorkOrderVO;
import com.mes.workorder.service.IWorkOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import com.mes.framework.es.ElasticsearchConfig.EsIndexNameResolver;
import com.mes.framework.tenant.TenantContextHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * 工单富查询实现（P2-28）
 *
 * <p>核心策略：</p>
 * <ol>
 *   <li>ES 启用 + 查询路径正常 → 走 ES 返回结果</li>
 *   <li>ES 未启用 / 查询抛异常 → 降级到 {@link IWorkOrderService#page(WorkOrderQuery)}</li>
 *   <li>无论主用还是降级，统一返回 {@link WorkOrderVO}，调用方无感知</li>
 * </ol>
 *
 * <p>依赖全部用 {@link ObjectProvider} 懒注入，确保 ES 未启用时本类也能正常实例化。</p>
 */
@Slf4j
@Service
public class WorkOrderSearchServiceImpl implements IWorkOrderSearchService {

    private static final String LOGICAL_INDEX = "mes_work_order";

    private final IWorkOrderService workOrderService;
    private final ObjectProvider<ElasticsearchOperations> esOpsProvider;
    private final ObjectProvider<EsIndexNameResolver> indexResolverProvider;

    public WorkOrderSearchServiceImpl(IWorkOrderService workOrderService,
                                      ObjectProvider<ElasticsearchOperations> esOpsProvider,
                                      ObjectProvider<EsIndexNameResolver> indexResolverProvider) {
        this.workOrderService = workOrderService;
        this.esOpsProvider = esOpsProvider;
        this.indexResolverProvider = indexResolverProvider;
    }

    @Override
    public PageResult<WorkOrderVO> queryRich(WorkOrderQuery query) {
        ElasticsearchOperations esOps = esOpsProvider.getIfAvailable();
        EsIndexNameResolver indexResolver = indexResolverProvider.getIfAvailable();
        if (esOps == null || indexResolver == null) {
            // ES 未启用，直接走 MyBatis
            log.debug("[WorkOrderSearch] ES 未启用，降级到 MyBatis 查询");
            return workOrderService.page(query);
        }

        try {
            return searchInEs(esOps, indexResolver, query);
        } catch (Exception ex) {
            // ES 不可用或查询异常时降级；只 WARN 不抛，避免影响业务
            log.warn("[WorkOrderSearch] ES 查询失败，降级到 MyBatis：{}", ex.getMessage());
            return workOrderService.page(query);
        }
    }

    /**
     * 使用 Native ES Query 查询。
     *
     * @param esOps         ES 操作模板
     * @param indexResolver 索引名解析器
     * @param query         查询参数
     * @return 分页结果
     */
    private PageResult<WorkOrderVO> searchInEs(ElasticsearchOperations esOps,
                                               EsIndexNameResolver indexResolver,
                                               WorkOrderQuery query) {
        Long tenantId = TenantContextHolder.getTenantId();
        String indexName = indexResolver.resolve(LOGICAL_INDEX, tenantId);

        NativeQueryBuilder builder = NativeQuery.builder();

        // 构建 bool 查询：所有条件按 should / must 组合
        Query boolQuery = buildBoolQuery(query, tenantId);
        builder.withQuery(boolQuery);

        // 分页与排序（按创建时间倒序，与 MyBatis 侧保持一致）
        int pageNum = Math.max(query.getPageNum(), 1);
        int pageSize = Math.max(query.getPageSize(), 1);
        builder.withPageable(PageRequest.of(pageNum - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "createdTime")));

        NativeQuery nativeQuery = builder.build();

        SearchHits<WorkOrderDoc> hits = esOps.search(nativeQuery, WorkOrderDoc.class,
                IndexCoordinates.of(indexName));

        List<WorkOrderVO> voList = new ArrayList<>(hits.getSearchHits().size());
        for (SearchHit<WorkOrderDoc> hit : hits.getSearchHits()) {
            voList.add(toVO(hit.getContent()));
        }

        return PageResult.of(voList, hits.getTotalHits());
    }

    /**
     * 构建 bool 查询，字段与 {@link WorkOrderQuery} 语义一一对应。
     *
     * @param query    查询参数
     * @param tenantId 当前租户 ID
     * @return 组合后的 ES Query
     */
    private Query buildBoolQuery(WorkOrderQuery query, Long tenantId) {
        return Query.of(q -> q.bool(b -> {
            if (tenantId != null) {
                b.filter(f -> f.term(t -> t.field("tenantId").value(tenantId)));
            }
            if (StringUtils.hasText(query.getWorkOrderNo())) {
                b.must(m -> m.wildcard(w -> w.field("workOrderNo")
                        .value("*" + query.getWorkOrderNo() + "*")));
            }
            if (StringUtils.hasText(query.getOrderNo())) {
                b.must(m -> m.wildcard(w -> w.field("orderNo")
                        .value("*" + query.getOrderNo() + "*")));
            }
            if (StringUtils.hasText(query.getProductCode())) {
                b.must(m -> m.term(t -> t.field("productCode").value(query.getProductCode())));
            }
            if (StringUtils.hasText(query.getProductName())) {
                b.must(m -> m.match(ma -> ma.field("productName").query(query.getProductName())));
            }
            if (StringUtils.hasText(query.getStatus())) {
                b.filter(f -> f.term(t -> t.field("status").value(query.getStatus())));
            }
            if (StringUtils.hasText(query.getMachineModel())) {
                b.filter(f -> f.term(t -> t.field("machineModel").value(query.getMachineModel())));
            }
            return b;
        }));
    }

    /**
     * Doc → VO 字段复制。
     * Doc 为精简字段，未填的 VO 字段保持 null，调用方如需完整字段走详情接口。
     *
     * @param doc ES 文档
     * @return 视图对象
     */
    private WorkOrderVO toVO(WorkOrderDoc doc) {
        WorkOrderVO vo = new WorkOrderVO();
        BeanUtils.copyProperties(doc, vo);
        return vo;
    }
}
