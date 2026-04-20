package com.mes.framework.routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;

/**
 * 数据源路由骨架：为未来的"大客户 Silo 模式"预留切换能力。
 *
 * <p>默认配置下所有租户都指向 {@code POOL}（共享 MySQL 实例），大客户上线时：</p>
 * <ol>
 *   <li>在 {@code sys_tenant.schema_mode} 设为 {@code SCHEMA} 或 {@code DB}；</li>
 *   <li>通过 {@link #registerTenantDataSource(Long, DataSource)} 注册租户独立数据源；</li>
 *   <li>调用 {@link #reloadTargets()} 使路由表生效；</li>
 *   <li>业务线程在进入时 {@link TenantRoutingContext#set(String)} 选择当前租户的路由键。</li>
 * </ol>
 *
 * <p><strong>当前阶段仅落脚手架，默认所有请求都走 {@code POOL}。</strong> 真正启用 Silo
 * 需配套：① 数据迁移工具 ② 租户配置中心（Nacos 或等价物） ③ 路由键 leader 选举。</p>
 */
@Slf4j
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    /** 路由键 -> DataSource，由 {@code DataSourceConfig} 在启动时写入 */
    private final Map<Object, Object> targets;

    public TenantRoutingDataSource(Map<Object, Object> targets, DataSource defaultPool) {
        this.targets = targets;
        super.setTargetDataSources(targets);
        super.setDefaultTargetDataSource(defaultPool);
        super.afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String routingKey = TenantRoutingContext.get();
        if (routingKey == null) {
            return "POOL"; // 默认共享库
        }
        return routingKey;
    }

    public synchronized void registerTenantDataSource(Long tenantId, DataSource ds) {
        String key = "TENANT_" + tenantId;
        targets.put(key, ds);
        reloadTargets();
        log.info("[TenantRouting] 注册租户独立数据源: tenantId={}, key={}", tenantId, key);
    }

    public synchronized void reloadTargets() {
        super.setTargetDataSources(targets);
        super.afterPropertiesSet();
    }
}
