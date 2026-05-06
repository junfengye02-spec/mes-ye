package com.mes.framework.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.common.result.R;
import com.mes.common.result.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final StaffPortalRestrictionFilter staffPortalRestrictionFilter;
    private final HmacSignatureFilter hmacSignatureFilter;
    private final ObjectMapper objectMapper;

    /**
     * 完全公开的接口（白名单）。
     *
     * <p>P0-04 安全整改（mcp7 执行）：移除 /files/**、/actuator/**、/druid/** 三条高危白名单。</p>
     * <ul>
     *   <li>/files/**：改为 authenticated，只有登录用户能访问租户文件</li>
     *   <li>/druid/**：完全移除，Druid 走自身 login-username/password 的 stat-view-servlet 登录</li>
     *   <li>/actuator/**：拆分到 ACTUATOR_PUBLIC_URLS 下面，只放行 health/info 两个无敏感信息端点</li>
     *   <li>/aps/callback/**：暂留 permitAll，但由 HMAC 签名 filter（P0-12，mcp10 实现）接管真实鉴权。
     *       HMAC filter 上线后本条必须同步移除。</li>
     * </ul>
     */
    private static final String[] PUBLIC_URLS = {
            "/auth/login",
            "/auth/refresh",
            "/auth/captcha",
            // Swagger / Knife4j（生产已通过 knife4j.enable=false 关闭接口文档，此处仅开发环境生效）
            "/doc.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**",
            "/favicon.ico",
            // APS 回调端点：由 HmacSignatureFilter（P0-12）做真实鉴权，无 JWT 也能进入 filter chain
            // TODO mcp10 完成 P0-12 后，可改为不走 PUBLIC_URLS，由 HmacSignatureFilter 完全接管
            "/aps/callback/**",
    };

    /**
     * Actuator 仅放行 health / info 两个端点给匿名访问，其余（env/metrics/prometheus 等）必须登录。
     *
     * <p>生产环境另外通过 application-prod.yml 的 management.endpoints.web.exposure.include 控制实际暴露范围。</p>
     */
    private static final String[] ACTUATOR_PUBLIC_URLS = {
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_URLS).permitAll()
                    .requestMatchers(ACTUATOR_PUBLIC_URLS).permitAll()
                    .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                        response.getWriter().write(objectMapper.writeValueAsString(
                                R.fail(ResultCode.UNAUTHORIZED)));
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                        response.getWriter().write(objectMapper.writeValueAsString(
                                R.fail(ResultCode.FORBIDDEN)));
                    })
            )
            // APS 回调 HMAC 校验必须在 JWT 校验之前完成：因为 /aps/callback/** 在 permitAll，
            // 不会走 JWT 拦截，必须由 HmacSignatureFilter 负责身份校验
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(hmacSignatureFilter, JwtAuthenticationFilter.class)
            .addFilterAfter(staffPortalRestrictionFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
