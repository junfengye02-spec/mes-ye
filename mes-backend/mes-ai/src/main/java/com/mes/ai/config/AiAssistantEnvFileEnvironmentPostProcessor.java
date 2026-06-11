package com.mes.ai.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AiAssistantEnvFileEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String ENV_FILE_NAME = ".env.ai.local";
    private static final String PROPERTY_SOURCE_NAME = "aiAssistantLocalEnv";
    private static final Pattern ENV_KEY = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final int MAX_PARENT_SEARCH_DEPTH = 5;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        loadFromWorkingDirectory(environment, Path.of("").toAbsolutePath().normalize());
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    static void loadFromWorkingDirectory(ConfigurableEnvironment environment, Path workingDirectory) {
        for (Path candidate : candidateFiles(workingDirectory)) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            Map<String, Object> values = parseEnvFile(candidate, environment);
            if (!values.isEmpty()) {
                addPropertySource(environment, candidate, values);
            }
            return;
        }
    }

    private static List<Path> candidateFiles(Path workingDirectory) {
        List<Path> candidates = new java.util.ArrayList<>();
        Path current = workingDirectory;
        for (int depth = 0; current != null && depth < MAX_PARENT_SEARCH_DEPTH; depth++) {
            candidates.add(current.resolve(ENV_FILE_NAME).normalize());
            current = current.getParent();
        }
        return candidates;
    }

    private static Map<String, Object> parseEnvFile(Path file, ConfigurableEnvironment environment) {
        Map<String, Object> values = new LinkedHashMap<>();
        try {
            for (String rawLine : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = stripBom(rawLine).trim();
                if (!StringUtils.hasText(line) || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("export ")) {
                    line = line.substring("export ".length()).trim();
                }
                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = line.substring(0, separator).trim();
                if (!ENV_KEY.matcher(key).matches() || environment.containsProperty(key)) {
                    continue;
                }
                values.put(key, unquote(line.substring(separator + 1).trim()));
            }
        } catch (IOException ignored) {
            return Map.of();
        }
        return values;
    }

    private static String stripBom(String value) {
        if (!value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static void addPropertySource(ConfigurableEnvironment environment, Path file, Map<String, Object> values) {
        MutablePropertySources sources = environment.getPropertySources();
        MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME + ":" + file, values);
        if (sources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, propertySource);
        } else {
            sources.addLast(propertySource);
        }
    }
}
