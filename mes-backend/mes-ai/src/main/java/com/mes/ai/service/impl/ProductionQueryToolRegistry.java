package com.mes.ai.service.impl;

import com.mes.ai.domain.dto.AiChatRequest;
import com.mes.ai.domain.model.AiIntent;
import com.mes.ai.domain.model.AiToolResult;
import com.mes.ai.service.AiToolRegistry;
import com.mes.common.core.PageResult;
import com.mes.query.domain.query.InspectionWorkQuery;
import com.mes.query.domain.query.ProductionWorkQuery;
import com.mes.query.domain.query.WorkStatusViewQuery;
import com.mes.query.domain.vo.InspectionWorkVO;
import com.mes.query.domain.vo.ProductionWorkVO;
import com.mes.query.domain.vo.WorkStatusViewVO;
import com.mes.query.service.IInspectionWorkService;
import com.mes.query.service.IProductionWorkService;
import com.mes.query.service.IWorkStatusViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductionQueryToolRegistry implements AiToolRegistry {

    private static final int QUERY_LIMIT = 5;

    private final IProductionWorkService productionWorkService;
    private final IInspectionWorkService inspectionWorkService;
    private final IWorkStatusViewService workStatusViewService;

    @Override
    public List<AiToolResult> collect(AiChatRequest request, AiIntent intent) {
        if (intent != AiIntent.PRODUCTION_QUERY && intent != AiIntent.QUALITY_STATUS) {
            return List.of();
        }
        String question = request.getQuestion() == null ? "" : request.getQuestion().toLowerCase(Locale.ROOT);
        List<AiToolResult> results = new ArrayList<>();
        if (question.contains("生产作业") || question.contains("生产工作") || question.contains("未完成")) {
            results.add(queryProductionWork(question));
        }
        if (question.contains("检验") || question.contains("质量")) {
            results.add(queryInspectionWork(question));
        }
        if (question.contains("工作状态") || question.contains("六状态") || question.contains("状态")) {
            results.add(queryWorkStatus(question));
        }
        return results.stream().filter(r -> r.summary() != null && !r.summary().isBlank()).toList();
    }

    private AiToolResult queryProductionWork(String question) {
        ProductionWorkQuery query = new ProductionWorkQuery();
        query.setPageNum(1);
        query.setPageSize(QUERY_LIMIT);
        PageResult<ProductionWorkVO> page = productionWorkService.page(query);
        return new AiToolResult("生产作业", summarize("生产作业", page.getTotal()), List.of("工作查询", "生产工单"));
    }

    private AiToolResult queryInspectionWork(String question) {
        InspectionWorkQuery query = new InspectionWorkQuery();
        query.setPageNum(1);
        query.setPageSize(QUERY_LIMIT);
        PageResult<InspectionWorkVO> page = inspectionWorkService.page(query);
        return new AiToolResult("检验工作", summarize("检验工作", page.getTotal()), List.of("工作查询", "质量管理"));
    }

    private AiToolResult queryWorkStatus(String question) {
        WorkStatusViewQuery query = new WorkStatusViewQuery();
        query.setPageNum(1);
        query.setPageSize(QUERY_LIMIT);
        PageResult<WorkStatusViewVO> page = workStatusViewService.page(query);
        return new AiToolResult("工作状态", summarize("工作状态记录", page.getTotal()), List.of("工作查询"));
    }

    private String summarize(String label, Long total) {
        long count = total == null ? 0L : total;
        return "当前权限范围内找到 " + count + " 条" + label + "。";
    }
}
