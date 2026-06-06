package com.mes.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.ai.config.AiAssistantProperties;
import com.mes.ai.domain.model.AiModelPrompt;
import com.mes.ai.service.AiModelClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurableAiModelClient implements AiModelClient {

    private final AiAssistantProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public String complete(AiModelPrompt prompt) {
        if (!isConfigured()) {
            return fallback(prompt);
        }
        try {
            RestTemplate restTemplate = new RestTemplate();
            String endpoint = properties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getApiKey());
            Map<String, Object> body = Map.of(
                    "model", properties.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt()),
                            Map.of("role", "user", "content", userPrompt(prompt))
                    ),
                    "temperature", 0.2,
                    "max_tokens", Math.max(256, properties.getMaxAnswerChars() / 2)
            );
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, new HttpEntity<>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String answer = root.path("choices").path(0).path("message").path("content").asText("");
            if (answer.length() > properties.getMaxAnswerChars()) {
                return answer.substring(0, properties.getMaxAnswerChars());
            }
            return answer;
        } catch (Exception e) {
            log.warn("[AI] 模型调用失败，降级为项目内回答: provider={}, model={}, timeout={}s, err={}",
                    properties.getProvider(), properties.getModel(), Duration.ofSeconds(properties.getTimeoutSeconds()).toSeconds(),
                    e.getMessage());
            return fallback(prompt);
        }
    }

    @Override
    public boolean isConfigured() {
        return properties.isEnabled()
                && StringUtils.hasText(properties.getBaseUrl())
                && StringUtils.hasText(properties.getApiKey())
                && StringUtils.hasText(properties.getModel());
    }

    private String systemPrompt() {
        return """
                你是MES生产助手。只能回答生产执行相关问题。
                回答必须基于给定项目上下文和授权查询证据。
                不得展示代码、SQL、接口路径、系统配置、密钥、堆栈或内部实现。
                不得编造系统没有提供的数据。
                如果问题越权、越界或要求写操作，必须拒答。
                """;
    }

    private String userPrompt(AiModelPrompt prompt) {
        return "问题：" + prompt.question()
                + "\n意图：" + prompt.intent()
                + "\n项目上下文：" + prompt.projectContext()
                + "\n查询证据：" + String.join("；", prompt.toolEvidence());
    }

    private String fallback(AiModelPrompt prompt) {
        if (!prompt.toolEvidence().isEmpty()) {
            return "根据当前系统查询结果：" + String.join("；", prompt.toolEvidence());
        }
        return "根据项目业务说明：" + prompt.projectContext();
    }
}
