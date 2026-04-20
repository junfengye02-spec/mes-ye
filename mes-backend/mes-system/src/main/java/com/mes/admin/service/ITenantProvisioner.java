package com.mes.admin.service;

/**
 * 租户初始化（Provisioning）服务。
 *
 * <p>Provision 分两种入口：</p>
 * <ul>
 *   <li>{@link #register}：完整的"注册 → 入库 → 异步初始化"闭环，用于租户自助注册或平台新建；</li>
 *   <li>{@link #provisionInternal}：同步初始化（内部），平台后台"重试"时调用；</li>
 *   <li>{@link #provisionAsync}：把 {@link #provisionInternal} 放到线程池里异步执行。</li>
 * </ul>
 */
public interface ITenantProvisioner {

    /**
     * 新租户注册入口。会：
     * <ol>
     *   <li>校验 tenantCode 未被占用；</li>
     *   <li>插入 {@code sys_tenant}（状态 PROVISIONING）；</li>
     *   <li>异步执行 {@link #provisionAsync}。</li>
     * </ol>
     */
    void register(String tenantCode, String tenantName, String contactName,
                  String contactEmail, String initialAdminUsername, String initialAdminPassword);

    /** 异步完成模板克隆 + 管理员创建 + 配额初始化 + 状态 ACTIVE。 */
    void provisionAsync(Long tenantId, String initialAdminUsername, String initialAdminPassword);

    /** 同步版本，供内部与"重试"调用。 */
    void provisionInternal(Long tenantId, String initialAdminUsername, String initialAdminPassword);
}
