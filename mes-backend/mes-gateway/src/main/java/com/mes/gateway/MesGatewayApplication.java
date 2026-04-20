package com.mes.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

@Slf4j
@EnableDiscoveryClient
@SpringBootApplication
public class MesGatewayApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(MesGatewayApplication.class, args);
        Environment env = ctx.getEnvironment();
        String port = env.getProperty("server.port", "8080");
        String serviceName = env.getProperty("spring.application.name", "mes-gateway");
        String profile = String.join(",", env.getActiveProfiles());
        if (profile.isEmpty()) {
            profile = String.join(",", env.getDefaultProfiles());
        }
        log.info("""
                
                ================================================
                    MES Gateway 启动成功!
                    Profile: {}
                    端口: {}
                    服务: {}
                ================================================
                """, profile, port, serviceName);
    }
}
