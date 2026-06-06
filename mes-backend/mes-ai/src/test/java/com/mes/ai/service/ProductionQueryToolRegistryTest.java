package com.mes.ai.service;

import com.mes.ai.domain.dto.AiChatRequest;
import com.mes.ai.domain.model.AiIntent;
import com.mes.ai.domain.model.AiToolResult;
import com.mes.ai.service.impl.ProductionQueryToolRegistry;
import com.mes.common.core.PageResult;
import com.mes.query.domain.query.InspectionWorkQuery;
import com.mes.query.domain.query.ProductionWorkQuery;
import com.mes.query.service.IInspectionWorkService;
import com.mes.query.service.IProductionWorkService;
import com.mes.query.service.IWorkStatusViewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionQueryToolRegistryTest {

    @Mock
    private IProductionWorkService productionWorkService;

    @Mock
    private IInspectionWorkService inspectionWorkService;

    @Mock
    private IWorkStatusViewService workStatusViewService;

    @Captor
    private ArgumentCaptor<ProductionWorkQuery> productionQueryCaptor;

    @Captor
    private ArgumentCaptor<InspectionWorkQuery> inspectionQueryCaptor;

    @Test
    void queriesOnlyWhitelistedProductionWorkWithSmallPageLimit() {
        when(productionWorkService.page(any(ProductionWorkQuery.class))).thenReturn(PageResult.of(List.of(), 7L));
        ProductionQueryToolRegistry registry = new ProductionQueryToolRegistry(
                productionWorkService, inspectionWorkService, workStatusViewService);

        List<AiToolResult> results = registry.collect(
                new AiChatRequest("查询未完成的生产作业", null, null), AiIntent.PRODUCTION_QUERY);

        verify(productionWorkService).page(productionQueryCaptor.capture());
        assertThat(productionQueryCaptor.getValue().getPageNum()).isEqualTo(1);
        assertThat(productionQueryCaptor.getValue().getPageSize()).isEqualTo(5);
        assertThat(results).singleElement()
                .satisfies(result -> {
                    assertThat(result.summary()).contains("7 条生产作业");
                    assertThat(result.relatedModules()).contains("工作查询", "生产工单");
                });
        verifyNoInteractions(inspectionWorkService, workStatusViewService);
    }

    @Test
    void qualityQuestionsUseInspectionToolWithSmallPageLimit() {
        when(inspectionWorkService.page(any(InspectionWorkQuery.class))).thenReturn(PageResult.of(List.of(), 3L));
        ProductionQueryToolRegistry registry = new ProductionQueryToolRegistry(
                productionWorkService, inspectionWorkService, workStatusViewService);

        List<AiToolResult> results = registry.collect(
                new AiChatRequest("今天有哪些检验工作需要关注？", null, null), AiIntent.QUALITY_STATUS);

        verify(inspectionWorkService).page(inspectionQueryCaptor.capture());
        assertThat(inspectionQueryCaptor.getValue().getPageNum()).isEqualTo(1);
        assertThat(inspectionQueryCaptor.getValue().getPageSize()).isEqualTo(5);
        assertThat(results).singleElement()
                .satisfies(result -> assertThat(result.summary()).contains("3 条检验工作"));
        verifyNoInteractions(productionWorkService, workStatusViewService);
    }

    @Test
    void ignoresNonProductionIntentWithoutCallingBusinessServices() {
        ProductionQueryToolRegistry registry = new ProductionQueryToolRegistry(
                productionWorkService, inspectionWorkService, workStatusViewService);

        List<AiToolResult> results = registry.collect(
                new AiChatRequest("系统有哪些模块？", null, null), AiIntent.MES_CONSULTATION);

        assertThat(results).isEmpty();
        verifyNoInteractions(productionWorkService, inspectionWorkService, workStatusViewService);
    }
}
