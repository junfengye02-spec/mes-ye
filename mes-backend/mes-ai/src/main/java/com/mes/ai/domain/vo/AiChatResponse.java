package com.mes.ai.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI助手问答响应")
public class AiChatResponse {

    @Schema(description = "回答正文")
    private String answer;

    @Schema(description = "识别到的意图")
    private String intent;

    @Builder.Default
    @Schema(description = "相关业务模块")
    private List<String> relatedModules = new ArrayList<>();

    @Builder.Default
    @Schema(description = "证据摘要")
    private List<String> evidenceSummary = new ArrayList<>();

    @Builder.Default
    @Schema(description = "建议跳转路径")
    private List<String> suggestedNavigation = new ArrayList<>();

    @Schema(description = "拒答原因")
    private String refusalReason;

    @Schema(description = "模型是否已配置")
    private Boolean modelConfigured;
}
