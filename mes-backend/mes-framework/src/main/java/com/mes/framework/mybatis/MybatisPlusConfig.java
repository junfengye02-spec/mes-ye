package com.mes.framework.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.mes.framework.security.SecurityUtils;
import com.mes.framework.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MyBatis-Plus 全局配置：租户列注入 + 公共字段自动填充。
 *
 * <p>多租户 fail-closed 策略：若当前请求未设置租户上下文且被拦截的表需要租户条件，
 * 直接抛出异常——避免默认 tenant_id=1 兜底造成跨租户数据泄露。</p>
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    /**
     * 默认跳过租户拦截的表：
     * <ul>
     *   <li>{@code sys_tenant}：租户元数据本身跨租户存在，不能被 WHERE tenant_id 限制。</li>
     *   <li>{@code sys_menu/role/role_menu/user_role}：暂时保留全局 RBAC（M2 阶段会租户化）。</li>
     *   <li>{@code sys_user}：用户表的 tenant_id 由业务代码显式过滤，避免加 where 导致的"改密码改到别的租户"。</li>
     *   <li>{@code mes_aps_sync_config}：APS 同步配置目前按工厂分。</li>
     * </ul>
     * 可通过 {@code mes.tenant.ignore-tables} 逗号分隔覆盖，覆盖时完全替换默认值。
     */
    private static final Set<String> DEFAULT_IGNORE_TABLES = Set.of(
            "sys_tenant", "sys_menu", "sys_role", "sys_role_menu", "sys_user_role",
            "sys_user", "mes_aps_sync_config"
    );

    private final Set<String> ignoreTables;

    public MybatisPlusConfig(@Value("${mes.tenant.ignore-tables:}") String override) {
        if (override == null || override.isBlank()) {
            this.ignoreTables = DEFAULT_IGNORE_TABLES;
        } else {
            this.ignoreTables = Arrays.stream(override.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(HashSet::new));
        }
        log.info("MybatisPlus 租户拦截忽略表: {}", ignoreTables);
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContextHolder.getTenantId();
                if (tenantId == null) {
                    throw new IllegalStateException(
                            "访问租户隔离表时未设置 TenantContext；这是一条 fail-closed 规则。"
                            + " 请检查：① 请求是否经过 JwtAuthenticationFilter；"
                            + " ② 异步任务/定时任务是否使用 TenantContextHolder.runAs() 或 TenantAwareExecutor；"
                            + " ③ 若为全局基础设施表，应加入 mes.tenant.ignore-tables。");
                }
                return new LongValue(tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return tableName != null && ignoreTables.contains(tableName.toLowerCase());
            }
        });
        interceptor.addInnerInterceptor(tenantInterceptor);
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {

            private String getCurrentUsername() {
                return Optional.ofNullable(SecurityUtils.getCurrentUsername()).orElse("system");
            }

            private Long getCurrentTenantIdOrThrow() {
                Long tenantId = TenantContextHolder.getTenantId();
                if (tenantId == null) {
                    throw new IllegalStateException(
                            "BaseEntity.tenantId 自动填充时未取到 TenantContext；"
                            + "入口 Filter 未设置或异步任务未透传。拒绝写入以防跨租户错挂。");
                }
                return tenantId;
            }

            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdTime", LocalDateTime::now, LocalDateTime.class);
                this.strictInsertFill(metaObject, "updatedTime", LocalDateTime::now, LocalDateTime.class);
                this.strictInsertFill(metaObject, "createdBy", this::getCurrentUsername, String.class);
                this.strictInsertFill(metaObject, "updatedBy", this::getCurrentUsername, String.class);
                this.strictInsertFill(metaObject, "tenantId", this::getCurrentTenantIdOrThrow, Long.class);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedTime", LocalDateTime::now, LocalDateTime.class);
                this.strictUpdateFill(metaObject, "updatedBy", this::getCurrentUsername, String.class);
            }
        };
    }
}
