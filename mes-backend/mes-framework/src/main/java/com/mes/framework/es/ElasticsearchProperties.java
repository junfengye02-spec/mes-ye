package com.mes.framework.es;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * MES Elasticsearch 连接属性（P2-28）
 *
 * <p>绑定前缀：{@code mes.es}。仅在 {@code mes.es.enabled=true} 时由
 * {@link ElasticsearchConfig} 装配真正的客户端与 Template。</p>
 *
 * <p>设计意图：</p>
 * <ul>
 *   <li>ES 作为 MySQL 模糊查询的加速层，不是唯一存储；默认关闭，业务侧显式开启</li>
 *   <li>连接配置、认证、超时全部外部化，prod 必须通过环境变量注入</li>
 *   <li>支持多节点地址（ES 集群），任何一个节点连不上会由客户端自动切换</li>
 * </ul>
 */
@Data
@ConfigurationProperties(prefix = "mes.es")
public class ElasticsearchProperties {

    /**
     * 是否启用 ES 集成，默认 false。
     * 关闭时不会注册任何 ES 相关 Bean，对现有启动路径零影响。
     */
    private boolean enabled = false;

    /**
     * 节点地址列表，形如 {@code ["http://es01:9200","http://es02:9200"]}。
     * 生产建议至少 3 个节点，开发可只配 1 个。
     */
    private List<String> uris = List.of("http://localhost:9200");

    /**
     * 认证用户名，生产环境建议开启 ES 安全并配置。
     */
    private String username;

    /**
     * 认证密码，生产必须通过环境变量注入，不得写死。
     */
    private String password;

    /**
     * 连接超时（建立 TCP 连接的超时时间）。
     */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * Socket 读超时（单个请求等待响应的最大时间）。
     * 设置不要过长，避免查询慢时阻塞业务线程。
     */
    private Duration socketTimeout = Duration.ofSeconds(10);

    /**
     * 最大连接数，按业务 QPS 调整。
     */
    private int maxConnTotal = 50;

    /**
     * 每路由最大连接数。
     */
    private int maxConnPerRoute = 10;

    /**
     * 索引前缀，用于多环境隔离（例如 dev/test/prod 写不同索引）。
     * 最终索引名 = {prefix}{logicalIndex}-{tenantId}。
     */
    private String indexPrefix = "";
}
