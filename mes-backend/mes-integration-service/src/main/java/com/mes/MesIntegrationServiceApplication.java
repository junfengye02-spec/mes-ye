package com.mes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableScheduling
@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
public class MesIntegrationServiceApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(MesIntegrationServiceApplication.class, args);
        Environment env = ctx.getEnvironment();
        String port = env.getProperty("server.port", "9006");
        String serviceName = env.getProperty("spring.application.name", "mes-integration");
        String profile = String.join(",", env.getActiveProfiles());
        if (profile.isEmpty()) {
            profile = String.join(",", env.getDefaultProfiles());
        }
        log.info("""
                
                ================================================
                    MES 集成服务 启动成功!
                    Profile: {}
                    端口: {}
                    服务: {}
                ================================================
                """, profile, port, serviceName);
    }
}
