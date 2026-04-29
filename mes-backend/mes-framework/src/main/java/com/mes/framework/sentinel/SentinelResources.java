package com.mes.framework.sentinel;

/**
 * Sentinel 资源名常量
 *
 * <p>统一管理所有受保护的资源名，避免散落在业务代码里的魔法字符串。
 * 命名规范：<b>业务域:动作</b>，冒号分隔，方便在 Grafana/Sentinel Dashboard 里按前缀聚合。</p>
 *
 * <p>每个资源对应的默认限流阈值参见 {@link SentinelRuleInitializer}。</p>
 */
public final class SentinelResources {

    private SentinelResources() {
    }

    /** 登录接口：每 IP 10 QPS，防止爆破 */
    public static final String AUTH_LOGIN = "auth:login";

    /** 文件上传：每租户 5 QPS，防止小文件洪水 */
    public static final String FILE_UPLOAD = "file:upload";

    /** 工单分页查询：单机 200 QPS，防止扫表 */
    public static final String WORKORDER_LIST = "workorder:list";

    /** 派工单分页查询：单机 200 QPS */
    public static final String DISPATCH_TASK_PAGE = "dispatch:task:page";

    /** 任意导出接口：每租户 1 QPS，防止爆推 */
    public static final String ANY_EXPORT = "any:export";
}
