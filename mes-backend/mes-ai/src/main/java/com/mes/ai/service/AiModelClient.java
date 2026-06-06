package com.mes.ai.service;

import com.mes.ai.domain.model.AiModelPrompt;

@FunctionalInterface
public interface AiModelClient {

    String complete(AiModelPrompt prompt);

    default boolean isConfigured() {
        return true;
    }
}
