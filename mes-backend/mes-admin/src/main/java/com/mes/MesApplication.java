package com.mes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MES 系统启动类
 */
@Slf4j
@EnableAsync
@EnableScheduling
@EnableDiscoveryClient
@SpringBootApplication
public class MesApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(MesApplication.class, args);
        Environment env = ctx.getEnvironment();
        String port = env.getProperty("server.port", "9091");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String profile = String.join(",", env.getActiveProfiles());
        if (profile.isEmpty()) {
            profile = String.join(",", env.getDefaultProfiles());
        }
        log.info("""
                
                ================================================
                    MES 制造执行系统 启动成功!
                    Profile: {}
                    接口文档: http://localhost:{}{}/doc.html
                ================================================
                """, profile, port, contextPath);
    }
}
