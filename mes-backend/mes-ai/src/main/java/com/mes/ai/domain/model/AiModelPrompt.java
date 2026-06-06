package com.mes.ai.domain.model;

import java.util.List;

public record AiModelPrompt(
        String question,
        AiIntent intent,
        String projectContext,
        List<String> toolEvidence
) {
}
