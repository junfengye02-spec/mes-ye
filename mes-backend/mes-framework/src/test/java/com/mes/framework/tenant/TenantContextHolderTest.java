package com.mes.framework.tenant;

import com.mes.framework.cache.CacheKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多租户上下文相关单测：确保缺失上下文 fail-closed、runAs 正确恢复、
 * CacheKeys 强制带租户前缀、异步快照可用。
 */
class TenantContextHolderTest {

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("requireTenantId() 在未设置租户时抛 IllegalStateException（fail-closed）")
    void requireTenantId_shouldFailFast_whenUnset() {
        assertThrows(IllegalStateException.class, TenantContextHolder::requireTenantId);
    }

    @Test
    @DisplayName("runAs 后的上下文应被正确恢复")
    void runAs_shouldRestorePreviousTenant() {
        TenantContextHolder.setTenantId(10L);
        TenantContextHolder.runAs(20L, () -> assertEquals(20L, TenantContextHolder.requireTenantId()));
        assertEquals(10L, TenantContextHolder.requireTenantId());
    }

    @Test
    @DisplayName("runAs 执行异常时也能恢复")
    void runAs_shouldRestoreEvenOnThrow() {
        TenantContextHolder.setTenantId(10L);
        assertThrows(RuntimeException.class, () ->
                TenantContextHolder.runAs(20L, (Runnable) () -> { throw new RuntimeException("boom"); }));
        assertEquals(10L, TenantContextHolder.requireTenantId());
    }

    @Test
    @DisplayName("跨线程使用 snapshot 应透传租户上下文")
    void snapshot_shouldPropagateAcrossThreads() throws Exception {
        TenantContextHolder.setTenantId(42L);
        var snap = TenantContextHolder.snapshot();
        AtomicReference<Long> seen = new AtomicReference<>();
        Thread t = new Thread(snap.wrap(() -> seen.set(TenantContextHolder.getTenantId())));
        t.start();
        t.join();
        assertEquals(42L, seen.get());
    }

    @Test
    @DisplayName("CacheKeys.tenant() 缺上下文时 fail-fast")
    void cacheKeys_shouldFailWithoutContext() {
        assertThrows(IllegalStateException.class, () -> CacheKeys.tenant("auth:permissions", 1L));
    }

    @Test
    @DisplayName("CacheKeys 严格按 tenant:{id}:{module}:{...} 拼 key")
    void cacheKeys_shouldComposeExpectedKey() {
        TenantContextHolder.setTenantId(7L);
        assertEquals("tenant:7:auth:permissions:42", CacheKeys.tenant("auth:permissions", 42L));
        assertEquals("tenant:99:rl:qps:*", CacheKeys.tenantPattern(99L, "rl:qps"));
        assertEquals("platform:registry:123", CacheKeys.platform("registry", 123));
    }

    @Test
    @DisplayName("PLATFORM_TENANT_ID 可识别")
    void platformContext_detection() {
        TenantContextHolder.setTenantId(TenantContextHolder.PLATFORM_TENANT_ID);
        assertTrue(TenantContextHolder.isPlatform());
        TenantContextHolder.setTenantId(1L);
        assertFalse(TenantContextHolder.isPlatform());
    }
}
