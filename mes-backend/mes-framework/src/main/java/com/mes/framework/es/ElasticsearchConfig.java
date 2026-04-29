package com.mes.framework.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.util.StringUtils;

/**
 * MES Elasticsearch 客户端与 Repository 扫描配置（P2-28）
 *
 * <p>激活条件：仅当 {@code mes.es.enabled=true} 时装配所有 ES 相关 Bean。
 * 默认 false，不装配任何 Bean，与 Spring Boot ES 自动配置互不干扰（通过
 * application.yml 的 {@code spring.autoconfigure.exclude} 禁用默认 ES auto-config）。</p>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>不继承 {@code ElasticsearchConfiguration}，避免与 Spring Boot auto-config 相互争抢</li>
 *   <li>显式装配 {@link RestClient} → {@link ElasticsearchClient} → {@link ElasticsearchOperations} 全链路</li>
 *   <li>自定义连接池与超时参数，保证高 QPS 下不成为瓶颈</li>
 *   <li>Repository 扫描 {@code com.mes} 下所有包</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(prefix = "mes.es", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ElasticsearchProperties.class)
@EnableElasticsearchRepositories(basePackages = "com.mes")
public class ElasticsearchConfig {

    /**
     * 低层 {@link RestClient}：业务如需直接发 HTTP 请求可注入本 Bean。
     * 使用自定义 HttpClient 连接池，提升并发性能。
     *
     * @param properties 连接属性
     * @return RestClient
     */
    @Bean(destroyMethod = "close")
    public RestClient elasticsearchRestClient(ElasticsearchProperties properties) {
        HttpHost[] httpHosts = properties.getUris().stream()
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);

        RestClientBuilder builder = RestClient.builder(httpHosts);

        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.setMaxConnTotal(properties.getMaxConnTotal());
            httpClientBuilder.setMaxConnPerRoute(properties.getMaxConnPerRoute());
            if (StringUtils.hasText(properties.getUsername())) {
                CredentialsProvider provider = new BasicCredentialsProvider();
                provider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(
                                properties.getUsername(),
                                properties.getPassword() == null ? "" : properties.getPassword()));
                httpClientBuilder.setDefaultCredentialsProvider(provider);
            }
            return httpClientBuilder;
        });

        builder.setRequestConfigCallback(reqCfg -> reqCfg
                .setConnectTimeout((int) properties.getConnectTimeout().toMillis())
                .setSocketTimeout((int) properties.getSocketTimeout().toMillis()));

        return builder.build();
    }

    /**
     * 高层 {@link ElasticsearchClient}（ES Java Client 8.x 原生 API）。
     * 适用于复杂聚合、bulk 操作，Spring Data 之外的场景。
     *
     * @param restClient 上面的 RestClient
     * @return 高层客户端
     */
    @Bean
    public ElasticsearchClient elasticsearchJavaClient(RestClient restClient) {
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    /**
     * Spring Data Elasticsearch 的实体 → 文档转换器。
     *
     * @return 转换器
     */
    @Bean
    public ElasticsearchConverter elasticsearchConverter() {
        return new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
    }

    /**
     * {@link ElasticsearchOperations} 主 Bean，所有业务 Service 直接注入它。
     *
     * @param client    高层客户端
     * @param converter 转换器
     * @return 操作模板
     */
    @Bean
    public ElasticsearchOperations elasticsearchOperations(ElasticsearchClient client,
                                                           ElasticsearchConverter converter) {
        ElasticsearchTemplate template = new ElasticsearchTemplate(client, converter);
        // IMMEDIATE：写入立即 refresh，便于写后立刻查。性能敏感场景改为 NONE / WAIT_UNTIL
        template.setRefreshPolicy(RefreshPolicy.IMMEDIATE);
        return template;
    }

    /**
     * 索引名解析器：将逻辑索引名 + 租户 ID 拼成真实索引名，供业务层显式传
     * {@code IndexCoordinates.of(...)} 使用。
     *
     * @param properties 连接属性
     * @return 解析器
     */
    @Bean
    public EsIndexNameResolver esIndexNameResolver(ElasticsearchProperties properties) {
        return new EsIndexNameResolver(properties.getIndexPrefix());
    }

    /**
     * 简单索引名解析器。约定：{prefix}{logical}-{tenantId}
     */
    public static class EsIndexNameResolver {

        private final String prefix;

        public EsIndexNameResolver(String prefix) {
            this.prefix = prefix == null ? "" : prefix;
        }

        /**
         * 解析最终索引名。
         *
         * @param logical  逻辑索引名，如 mes_work_order
         * @param tenantId 租户 ID，为 null 时退回 default
         * @return 实际索引名，如 dev-mes_work_order-1001
         */
        public String resolve(String logical, Long tenantId) {
            String tenant = tenantId == null ? "default" : tenantId.toString();
            return prefix + logical + "-" + tenant;
        }

        public String getPrefix() {
            return prefix;
        }
    }
}
