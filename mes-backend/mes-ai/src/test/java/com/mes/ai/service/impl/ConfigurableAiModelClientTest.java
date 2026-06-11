package com.mes.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.ai.config.AiAssistantProperties;
import com.mes.ai.domain.model.AiIntent;
import com.mes.ai.domain.model.AiModelPrompt;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurableAiModelClientTest {

    @Test
    void doesNotReturnFallbackWhenConfiguredProviderFails() {
        AiAssistantProperties properties = new AiAssistantProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://127.0.0.1:9/v1");
        properties.setApiKey("sk-test-value");
        properties.setModel("gpt-test");
        properties.setTimeoutSeconds(1);
        ConfigurableAiModelClient client = new ConfigurableAiModelClient(properties, new ObjectMapper());

        assertThatThrownBy(() -> client.complete(new AiModelPrompt(
                "查询未完成的生产作业",
                AiIntent.PRODUCTION_QUERY,
                "项目上下文",
                List.of("找到 2 条未完成生产作业")
        )))
                .isInstanceOf(AiModelUnavailableException.class)
                .hasMessageContaining("模型调用失败");
    }
}
