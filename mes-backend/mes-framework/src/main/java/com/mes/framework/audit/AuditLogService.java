package com.mes.framework.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.framework.security.LoginUser;
import com.mes.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 审计日志落库服务。
 *
 * <p>注意事项：</p>
 * <ul>
 *   <li>默认通过 {@code mesDefaultExecutor} 异步写入，避免阻塞业务事务；</li>
 *   <li>写失败只 WARN 不抛，审计缺失不应影响业务；</li>
 *   <li>从 TenantContext / SecurityContext / MDC / HttpServletRequest 自动补全字段，
 *       调用方通常只需要给 {@link AuditEvent} 填 action / target。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Async("mesDefaultExecutor")
    public void log(AuditEvent event) {
        try {
            enrich(event);
            String payload = event.getPayload() != null
                    ? objectMapper.writeValueAsString(event.getPayload())
                    : null;
            jdbcTemplate.update(
                    "INSERT INTO sys_audit_log (" +
                        "tenant_id, operator_tenant_id, operator_user_id, operator_username," +
                        " action, target_type, target_id, trace_id, ip, user_agent," +
                        " payload_json, result, error_message, created_time)" +
                    " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                    event.getTenantId(),
                    event.getOperatorTenantId(),
                    event.getOperatorUserId(),
                    event.getOperatorUsername(),
                    event.getAction(),
                    event.getTargetType(),
                    event.getTargetId(),
                    event.getTraceId(),
                    event.getIp(),
                    event.getUserAgent(),
                    payload,
                    event.getResult() != null ? event.getResult() : "OK",
                    event.getErrorMessage());
        } catch (Exception e) {
            log.warn("[Audit] 写入审计日志失败（忽略）: action={}, err={}", event.getAction(), e.getMessage());
        }
    }

    private void enrich(AuditEvent e) {
        if (e.getTenantId() == null) e.setTenantId(TenantContextHolder.getTenantId());
        if (e.getOperatorTenantId() == null) e.setOperatorTenantId(TenantContextHolder.getTenantId());
        if (e.getTraceId() == null) e.setTraceId(MDC.get("traceId"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser lu) {
            if (e.getOperatorUserId() == null) e.setOperatorUserId(lu.getUserId());
            if (e.getOperatorUsername() == null) e.setOperatorUsername(lu.getUsername());
        }

        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                HttpServletRequest req = sra.getRequest();
                if (e.getIp() == null) e.setIp(resolveClientIp(req));
                if (e.getUserAgent() == null) e.setUserAgent(req.getHeader("User-Agent"));
            }
        } catch (Exception ignore) {
            // 异步线程中 RequestContextHolder 可能为空，忽略
        }
    }

    private String resolveClientIp(HttpServletRequest req) {
        String header = req.getHeader("X-Forwarded-For");
        if (header != null && !header.isBlank()) {
            int comma = header.indexOf(',');
            return (comma >= 0 ? header.substring(0, comma) : header).trim();
        }
        String real = req.getHeader("X-Real-IP");
        return real != null && !real.isBlank() ? real : req.getRemoteAddr();
    }
}
