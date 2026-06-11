package com.mes.ai.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.env.EnvironmentPostProcessorsFactory;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiAssistantEnvFileEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsAiEnvFileFromRepositoryRootWhenStartedInMesAdminDirectory() throws IOException {
        Path repoRoot = tempDir.resolve("repo");
        Path startDirectory = repoRoot.resolve("mes-backend/mes-admin");
        Files.createDirectories(startDirectory);
        Files.writeString(repoRoot.resolve(".env.ai.local"), """
                MES_AI_ASSISTANT_ENABLED=true
                MES_AI_ASSISTANT_BASE_URL=https://example.invalid/v1
                MES_AI_ASSISTANT_API_KEY=sk-test-value
                MES_AI_ASSISTANT_MODEL=gpt-test
                """);
        ConfigurableEnvironment environment = new StandardEnvironment();

        AiAssistantEnvFileEnvironmentPostProcessor.loadFromWorkingDirectory(environment, startDirectory);

        assertThat(environment.getProperty("MES_AI_ASSISTANT_ENABLED")).isEqualTo("true");
        assertThat(environment.getProperty("MES_AI_ASSISTANT_BASE_URL")).isEqualTo("https://example.invalid/v1");
        assertThat(environment.getProperty("MES_AI_ASSISTANT_API_KEY")).isEqualTo("sk-test-value");
        assertThat(environment.getProperty("MES_AI_ASSISTANT_MODEL")).isEqualTo("gpt-test");
    }

    @Test
    void keepsExistingEnvironmentValuesAboveLocalEnvFile() throws IOException {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.writeString(repoRoot.resolve(".env.ai.local"), "MES_AI_ASSISTANT_MODEL=file-model\n");
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test-env",
                Map.of("MES_AI_ASSISTANT_MODEL", "real-env-model")));

        AiAssistantEnvFileEnvironmentPostProcessor.loadFromWorkingDirectory(environment, repoRoot);

        assertThat(environment.getProperty("MES_AI_ASSISTANT_MODEL")).isEqualTo("real-env-model");
    }

    @Test
    void isRegisteredAsSpringBootEnvironmentPostProcessor() {
        var processors = EnvironmentPostProcessorsFactory.fromSpringFactories(getClass().getClassLoader())
                .getEnvironmentPostProcessors(new DeferredLogs(), new DefaultBootstrapContext());

        assertThat(processors).anyMatch(AiAssistantEnvFileEnvironmentPostProcessor.class::isInstance);
    }
}
