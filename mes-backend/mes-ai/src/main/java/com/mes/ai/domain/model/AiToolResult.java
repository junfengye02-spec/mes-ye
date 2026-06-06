package com.mes.ai.domain.model;

import java.util.List;

public record AiToolResult(
        String toolName,
        String summary,
        List<String> relatedModules
) {
}
