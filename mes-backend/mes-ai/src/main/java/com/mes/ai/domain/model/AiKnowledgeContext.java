package com.mes.ai.domain.model;

import java.util.List;

public record AiKnowledgeContext(
        String context,
        List<String> relatedModules,
        List<String> evidenceSummary,
        List<String> suggestedNavigation
) {
}
