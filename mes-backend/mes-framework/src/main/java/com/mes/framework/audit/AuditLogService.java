package com.mes.framework.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.framework.security.LoginUser;
import com.mes.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * 审计日志落库服务。
 *
 * <p>注意事项：</p>
 * <ul>
 *   <li>默认通过 {@code mesDefaultExecutor} 异步写入，避免阻塞业务事务；</li>
 *   <li>写失败只 WARN 不抛，审计缺失不应影响业务；</li>
 *   <li>从 TenantContext / SecurityContext / MDC / HttpServletRequest 自动补全字段，
 *       调用方通常只需要给 {@link AuditEvent} 填 action / target；</li>
 *   <li>P3-12：当 {@link AuditEvent#getOversizedResponse()} 或 {@link AuditEvent#getOversizedRequest()}
 *       非空时，主表写入后会把原文按 {@code mes.audit.chunk-size}（默认 64KB）分片写入
 *       {@code sys_audit_log_payload}，用于大导出 / 批量操作可溯源。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    /** 全局默认分片大小：64KB。 */
    private static final int DEFAULT_CHUNK_SIZE = 64 * 1024;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${mes.audit.chunk-size:" + DEFAULT_CHUNK_SIZE + "}")
    private int globalChunkSize;

    @Value("${mes.audit.payload-storage:DB}")
    private String payloadStorage;

    @Async("mesDefaultExecutor")
    public void log(AuditEvent event) {
        Long auditLogId = null;
        try {
            enrich(event);
            String payload = event.getPayload() != null
                    ? objectMapper.writeValueAsString(event.getPayload())
                    : null;
            auditLogId = insertMain(event, payload);
        } catch (Exception e) {
            log.warn("[Audit] 写入审计日志主表失败（忽略）: action={}, err={}",
                    event.getAction(), e.getMessage());
            return;
        }

        // 主表写入成功后，若有超阈值 payload 则落子表（失败不回滚主表）
        try {
            if (auditLogId != null && event.getOversizedResponse() != null) {
                savePayloadChunks(auditLogId, event.getTenantId(),
                        "RESPONSE", event.getOversizedResponse(), event.getPayloadChunkSize());
            }
            if (auditLogId != null && event.getOversizedRequest() != null) {
                savePayloadChunks(auditLogId, event.getTenantId(),
                        "REQUEST", event.getOversizedRequest(), event.getPayloadChunkSize());
            }
        } catch (Exception e) {
            log.warn("[Audit] 写入审计日志分片子表失败（忽略）: auditLogId={}, err={}",
                    auditLogId, e.getMessage());
        }
    }

    /**
     * {@link #log(AuditEvent)} 的语义别名（P1-13）。
     *
     * <p>AOP 切面 {@code AuditLogAspect} 调用本方法时语义更贴切，
     * 实际行为与 {@link #log(AuditEvent)} 完全一致——异步写入 sys_audit_log，写失败 WARN 不抛。</p>
     *
     * @param event 审计事件
     */
    public void recordAsync(AuditEvent event) {
        // 直接委托给 log()；切面统一通过本方法调用可以让业务语义更清晰
        log(event);
    }

    /**
     * 把超大 payload 按 {@code chunkSize} 字节切片写入 {@code sys_audit_log_payload}。
     *
     * <p>供切面在本服务异步任务外（例如纯同步场景）直接调用。public 暴露便于单测覆盖。</p>
     *
     * @param auditLogId 主表行 id
     * @param tenantId   租户 id（冗余到子表便于分区 / 归档）
     * @param payloadType RESPONSE / REQUEST / EXCEPTION
     * @param fullContent 原始完整 payload 文本
     * @param chunkSize   单片最大字节数；{@code &lt;=0} 则使用全局默认
     */
    public void savePayloadChunks(Long auditLogId, Long tenantId,
                                  String payloadType, String fullContent, int chunkSize) {
        if (auditLogId == null || fullContent == null || fullContent.isEmpty()) {
            return;
        }
        int effectiveChunkSize = chunkSize > 0 ? chunkSize : globalChunkSize;
        byte[] bytes = fullContent.getBytes(StandardCharsets.UTF_8);
        int total = bytes.length;
        int chunkTotal = (total + effectiveChunkSize - 1) / effectiveChunkSize;
        String sha256 = sha256Hex(bytes);
        long tid = tenantId != null ? tenantId : 0L;

        for (int seq = 0; seq < chunkTotal; seq++) {
            int from = seq * effectiveChunkSize;
            int to = Math.min(from + effectiveChunkSize, total);
            // 切片时按 UTF-8 字节切，落库时仍按 UTF-8 还原字符串，避免截断破坏码点
            String chunk = new String(bytes, from, to - from, StandardCharsets.UTF_8);
            try {
                jdbcTemplate.update(
                        "INSERT INTO sys_audit_log_payload (" +
                            "audit_log_id, tenant_id, payload_type," +
                            " chunk_seq, chunk_total, content_length, total_length," +
                            " content_sha256, storage_backend, content_chunk, created_time)" +
                        " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                        auditLogId, tid, payloadType,
                        seq, chunkTotal, to - from, (long) total,
                        sha256, "DB", chunk);
            } catch (Exception e) {
                log.warn("[Audit] 分片落库失败: auditLogId={}, type={}, seq={}/{}, err={}",
                        auditLogId, payloadType, seq, chunkTotal, e.getMessage());
                // 单片失败不阻塞后续片；严重故障由上层 try/catch 兜底
            }
        }
    }

    /**
     * 写入主表 {@code sys_audit_log} 并回填自增 id，供分片表引用。
     */
    private Long insertMain(AuditEvent event, String payload) {
        final String sql = "INSERT INTO sys_audit_log (" +
                "tenant_id, operator_tenant_id, operator_user_id, operator_username," +
                " action, target_type, target_id, trace_id, ip, user_agent," +
                " payload_json, result, error_message, created_time)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, event.getTenantId());
            ps.setObject(2, event.getOperatorTenantId());
            ps.setObject(3, event.getOperatorUserId());
            ps.setObject(4, event.getOperatorUsername());
            ps.setString(5, event.getAction());
            ps.setString(6, event.getTargetType());
            ps.setString(7, event.getTargetId());
            ps.setString(8, event.getTraceId());
            ps.setString(9, event.getIp());
            ps.setString(10, event.getUserAgent());
            ps.setString(11, payload);
            ps.setString(12, event.getResult() != null ? event.getResult() : "OK");
            ps.setString(13, event.getErrorMessage());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // JDK 必带 SHA-256，理论不可达
            return null;
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
