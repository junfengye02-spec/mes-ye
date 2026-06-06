package com.mes.ai.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.ai.domain.dto.AiChatRequest;
import com.mes.ai.domain.vo.AiChatResponse;
import com.mes.ai.service.AiAssistantService;
import com.mes.common.result.R;
import com.mes.framework.sentinel.MesRateLimit;
import com.mes.framework.sentinel.SentinelBlockHandlers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Tag(name = "AI助手", description = "生产问题自然语言问答")
@RestController
@RequestMapping("/ai/assistant")
@RequiredArgsConstructor
@Slf4j
public class AiAssistantController {

    private static final String RESOURCE = "ai:assistant:chat";
    private static final String STREAM_RESOURCE = "ai:assistant:chat:stream";
    private static final int STREAM_CHUNK_SIZE = 18;

    private final AiAssistantService aiAssistantService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "AI助手问答", description = "仅回答当前权限内的只读生产业务问题，不展示代码、SQL或内部配置")
    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('ai:assistant:chat')")
    @SentinelResource(value = RESOURCE, blockHandler = "handleR", blockHandlerClass = SentinelBlockHandlers.class)
    @MesRateLimit(resource = RESOURCE, key = MesRateLimit.Key.TENANT, count = 5)
    public R<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return R.ok(aiAssistantService.chat(request));
    }

    @Operation(summary = "AI助手流式问答", description = "以SSE分块返回已脱敏的生产业务回答")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('ai:assistant:chat')")
    @MesRateLimit(resource = STREAM_RESOURCE, key = MesRateLimit.Key.TENANT, count = 5)
    public StreamingResponseBody chatStream(@Valid @RequestBody AiChatRequest request) {
        AiChatResponse response = aiAssistantService.chat(request);
        return outputStream -> emitResponse(outputStream, response);
    }

    private void emitResponse(OutputStream outputStream, AiChatResponse response) throws IOException {
        try {
            String answer = StringUtils.hasText(response.getAnswer()) ? response.getAnswer() : "AI助手暂时没有可用回答。";
            for (String chunk : splitAnswer(answer)) {
                writeEvent(outputStream, "delta", Map.of("content", chunk));
            }
            writeEvent(outputStream, "done", response);
        } catch (Exception e) {
            log.warn("[AI] 流式输出失败: {}", e.getMessage());
            writeEvent(outputStream, "error", Map.of("message", "AI助手暂时不可用"));
        } finally {
            outputStream.flush();
            outputStream.close();
        }
    }

    private void writeEvent(OutputStream outputStream, String event, Object data) throws IOException {
        String frame = "event:" + event + "\n"
                + "data:" + objectMapper.writeValueAsString(data) + "\n\n";
        outputStream.write(frame.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private List<String> splitAnswer(String answer) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < answer.length(); i += STREAM_CHUNK_SIZE) {
            chunks.add(answer.substring(i, Math.min(answer.length(), i + STREAM_CHUNK_SIZE)));
        }
        return chunks;
    }
}
