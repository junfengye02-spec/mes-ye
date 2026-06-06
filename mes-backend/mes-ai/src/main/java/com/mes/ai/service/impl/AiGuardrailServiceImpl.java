package com.mes.ai.service.impl;

import com.mes.ai.service.AiGuardrailService;
import com.mes.ai.service.GuardrailDecision;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AiGuardrailServiceImpl implements AiGuardrailService {

    private static final List<String> CODE_PATTERNS = List.of(
            "代码", "源码", "source code", "sql", "select ", "insert ", "update ", "delete ",
            "application-prod", "application.yml", "配置", "api route", "接口路径", "接口地址",
            "secret", "password", "token", "堆栈", "stack trace"
    );

    private static final List<String> CROSS_TENANT_OR_WRITE_PATTERNS = List.of(
            "其他租户", "别的租户", "跨租户", "绕过权限", "无权限", "自动关闭", "自动审批",
            "自动下发", "删除", "修改", "新增", "创建", "提交", "关闭异常", "执行操作"
    );

    private static final Pattern API_PATH = Pattern.compile("(^|\\s)/api/[A-Za-z0-9_./-]+");
    private static final Pattern SQL_LINE = Pattern.compile("(?i).*\\b(select|insert|update|delete|drop|alter)\\b.*");
    private static final Pattern SECRET_LINE = Pattern.compile("(?i).*(password|secret|token|api[_-]?key)\\s*=.*");
    private static final Pattern CODE_LINE = Pattern.compile(".*(```|\\bclass\\s+|\\bpublic\\s+|\\bprivate\\s+|\\bfunction\\s+).*");

    @Override
    public GuardrailDecision inspectQuestion(String question) {
        if (!StringUtils.hasText(question)) {
            return GuardrailDecision.deny("问题不能为空。");
        }
        String normalized = normalize(question);
        if (containsAny(normalized, CROSS_TENANT_OR_WRITE_PATTERNS)) {
            return GuardrailDecision.deny("该问题涉及越权数据或写操作，AI助手只能回答当前权限内的只读生产问题。");
        }
        if (containsAny(normalized, CODE_PATTERNS)) {
            return GuardrailDecision.deny("不能展示代码、SQL、接口、配置或密钥等内部技术信息。");
        }
        return GuardrailDecision.allow();
    }

    @Override
    public String sanitizeAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return "当前没有可用于回答的生产证据。";
        }
        String[] lines = answer
                .replace("不要展示代码。", "")
                .replace("不要展示代码", "")
                .split("\\R");
        List<String> safeLines = new ArrayList<>();
        boolean hidden = false;
        for (String line : lines) {
            if (isUnsafeOutputLine(line)) {
                hidden = true;
                continue;
            }
            if (StringUtils.hasText(line)) {
                safeLines.add(line.trim());
            }
        }
        String sanitized = String.join("\n", safeLines).trim();
        if (!StringUtils.hasText(sanitized)) {
            sanitized = "我可以基于项目业务流程回答生产问题，但不能展示内部技术细节。";
        }
        if (hidden) {
            sanitized = sanitized + "\n已隐藏不适合在AI助手中展示的技术细节。";
        }
        return sanitized;
    }

    private boolean isUnsafeOutputLine(String line) {
        String normalized = normalize(line);
        return API_PATH.matcher(line).find()
                || SQL_LINE.matcher(line).matches()
                || SECRET_LINE.matcher(line).matches()
                || CODE_LINE.matcher(line).matches()
                || normalized.contains("application-prod")
                || normalized.contains("接口路径");
    }

    private boolean containsAny(String value, List<String> patterns) {
        return patterns.stream().anyMatch(value::contains);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
