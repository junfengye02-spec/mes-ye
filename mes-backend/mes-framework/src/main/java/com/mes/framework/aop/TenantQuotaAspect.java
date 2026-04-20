package com.mes.framework.aop;

import com.mes.common.exception.BusinessException;
import com.mes.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 配额切面：在目标方法执行前校验当前租户未超限，执行成功后累加用量。
 *
 * <p>实现上为了避免被 MyBatis-Plus 租户拦截器包裹（sys_tenant / sys_tenant_quota_usage
 * 本身会进入忽略名单，但为了稳健性这里用 {@link JdbcTemplate} 原生 SQL），
 * 并以 {@code INSERT ... ON DUPLICATE KEY UPDATE} 方式自增用量。</p>
 *
 * <p>超限时抛 {@link BusinessException}，HTTP 上表现为 400 / 业务码；
 * 网关层的 QPS 维度限流返回 429，两者互补。</p>
 */
@Slf4j
@Aspect
@Component
@Order(10)
@RequiredArgsConstructor
public class TenantQuotaAspect {

    private final JdbcTemplate jdbcTemplate;

    @Around("@annotation(quota)")
    public Object around(ProceedingJoinPoint pjp, TenantQuota quota) throws Throwable {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || TenantContextHolder.PLATFORM_TENANT_ID.equals(tenantId)) {
            // 平台超管 / 无租户上下文（系统任务）跳过配额
            return pjp.proceed();
        }

        long limit = fetchLimit(tenantId, quota.metric());
        long current = fetchUsage(tenantId, quota.metric());
        if (limit > 0 && current + quota.delta() > limit) {
            String msg = quota.errorMessage().isBlank()
                    ? String.format("租户 %d 的 %s 已达上限（%d / %d）", tenantId, quota.metric().name(), current, limit)
                    : quota.errorMessage();
            log.warn("[TenantQuota] 拦截超配额操作: tenantId={}, metric={}, current={}, delta={}, limit={}",
                    tenantId, quota.metric(), current, quota.delta(), limit);
            throw new BusinessException(msg);
        }

        Object result = pjp.proceed();
        incrementUsage(tenantId, quota.metric(), quota.delta());
        return result;
    }

    private long fetchLimit(Long tenantId, TenantQuotaMetric metric) {
        String sql = "SELECT " + metric.column() + " FROM sys_tenant WHERE id = ? AND deleted = 0";
        Map<String, Object> row = safeQueryForMap(sql, tenantId);
        if (row == null) return 0L;
        Object v = row.get(metric.column());
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    private long fetchUsage(Long tenantId, TenantQuotaMetric metric) {
        String sql = "SELECT value_current FROM sys_tenant_quota_usage WHERE tenant_id = ? AND metric = ?";
        Map<String, Object> row = safeQueryForMap(sql, tenantId, metric.name());
        if (row == null) return 0L;
        Object v = row.get("value_current");
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    private void incrementUsage(Long tenantId, TenantQuotaMetric metric, long delta) {
        String sql = "INSERT INTO sys_tenant_quota_usage (tenant_id, metric, value_current, value_peak) "
                + "VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE "
                + "  value_current = value_current + VALUES(value_current), "
                + "  value_peak = GREATEST(value_peak, value_current + VALUES(value_current))";
        try {
            jdbcTemplate.update(sql, tenantId, metric.name(), delta, delta);
        } catch (Exception e) {
            log.warn("[TenantQuota] 用量累加失败（不阻塞主流程）: tenantId={}, metric={}, err={}",
                    tenantId, metric, e.getMessage());
        }
    }

    private Map<String, Object> safeQueryForMap(String sql, Object... args) {
        try {
            return jdbcTemplate.queryForMap(sql, args);
        } catch (org.springframework.dao.EmptyResultDataAccessException ignored) {
            return null;
        } catch (Exception e) {
            log.warn("[TenantQuota] 查询配额失败（降级为 0）: sql={}, err={}", sql, e.getMessage());
            return null;
        }
    }
}
