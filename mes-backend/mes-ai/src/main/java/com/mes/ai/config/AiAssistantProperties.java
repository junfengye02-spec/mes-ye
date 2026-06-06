package com.mes.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mes.ai-assistant")
public class AiAssistantProperties {

    private boolean enabled = false;
    private String provider = "openai-compatible";
    private String baseUrl = "";
    private String apiKey = "";
    private String model = "";
    private int timeoutSeconds = 20;
    private int maxAnswerChars = 1200;
}
