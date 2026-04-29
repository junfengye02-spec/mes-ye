package com.mes.framework.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑声明。
 *
 * M9-P3-11 关键变更：
 *   1) 全部业务队列声明 x-queue-type=quorum（由 Raft 协议保证强一致）
 *   2) Quorum 队列天然跨节点复制（N/2+1 法定多数），不再依赖 classic mirrored
 *      queue + ha-all policy 的强一致保证；服务器端 definitions 里的 ha-all
 *      policy 已限制到 apply-to: classic_queues，避免干扰 quorum 队列。
 *   3) Quorum 支持 x-dead-letter-exchange / x-dead-letter-routing-key；
 *      DLQ 本身也用 quorum 类型（避免死信路径反而掉消息）
 *   4) 不兼容项：Quorum 不支持 x-max-priority / x-queue-mode=lazy / exclusive。
 *      本项目原声明未使用上述参数，迁移无需额外改造。
 *   5) 一次性通知场景（无需强一致、要求低延迟）仍可用 classic 队列，
 *      目前工程内不存在此类声明，不做保留。
 *
 * 迁移路径见 docs/operations/rabbitmq-quorum-migration.md
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_MES_TOPIC = "mes.topic";
    public static final String EXCHANGE_MES_DLX = "mes.dlx";

    public static final String QUEUE_APS_SYNC = "mes.aps.sync";
    public static final String QUEUE_WORKORDER_EVENTS = "mes.workorder.events";
    public static final String QUEUE_INVENTORY_EVENTS = "mes.inventory.events";
    public static final String QUEUE_QUALITY_EVENTS = "mes.quality.events";

    public static final String QUEUE_APS_SYNC_DLQ = "mes.aps.sync.dlq";
    public static final String QUEUE_WORKORDER_EVENTS_DLQ = "mes.workorder.events.dlq";

    public static final String RK_APS_SYNC = "aps.sync.#";
    public static final String RK_WORKORDER = "workorder.#";
    public static final String RK_INVENTORY = "inventory.#";
    public static final String RK_QUALITY = "quality.#";

    /**
     * Quorum 队列的 x-queue-type 参数常量，便于后续切换或做灰度。
     */
    private static final String ARG_QUEUE_TYPE = "x-queue-type";
    private static final String QUEUE_TYPE_QUORUM = "quorum";

    @Bean
    public TopicExchange mesTopicExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_MES_TOPIC).durable(true).build();
    }

    @Bean
    public TopicExchange mesDlxExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_MES_DLX).durable(true).build();
    }

    // ==================== Queues（全部 Quorum） ====================

    @Bean
    public Queue apsSyncQueue() {
        return QueueBuilder.durable(QUEUE_APS_SYNC)
                .withArgument(ARG_QUEUE_TYPE, QUEUE_TYPE_QUORUM)
                .withArgument("x-dead-letter-exchange", EXCHANGE_MES_DLX)
                .withArgument("x-dead-letter-routing-key", "dlq.aps.sync")
                .build();
    }

    @Bean
    public Queue workorderEventsQueue() {
        return QueueBuilder.durable(QUEUE_WORKORDER_EVENTS)
                .withArgument(ARG_QUEUE_TYPE, QUEUE_TYPE_QUORUM)
                .withArgument("x-dead-letter-exchange", EXCHANGE_MES_DLX)
                .withArgument("x-dead-letter-routing-key", "dlq.workorder")
                .build();
    }

    @Bean
    public Queue inventoryEventsQueue() {
        return QueueBuilder.durable(QUEUE_INVENTORY_EVENTS)
                .withArgument(ARG_QUEUE_TYPE, QUEUE_TYPE_QUORUM)
                .build();
    }

    @Bean
    public Queue qualityEventsQueue() {
        return QueueBuilder.durable(QUEUE_QUALITY_EVENTS)
                .withArgument(ARG_QUEUE_TYPE, QUEUE_TYPE_QUORUM)
                .build();
    }

    @Bean
    public Queue apsSyncDlq() {
        return QueueBuilder.durable(QUEUE_APS_SYNC_DLQ)
                .withArgument(ARG_QUEUE_TYPE, QUEUE_TYPE_QUORUM)
                .build();
    }

    @Bean
    public Queue workorderEventsDlq() {
        return QueueBuilder.durable(QUEUE_WORKORDER_EVENTS_DLQ)
                .withArgument(ARG_QUEUE_TYPE, QUEUE_TYPE_QUORUM)
                .build();
    }

    // ==================== Bindings ====================

    @Bean
    public Binding apsSyncBinding() {
        return BindingBuilder.bind(apsSyncQueue()).to(mesTopicExchange()).with(RK_APS_SYNC);
    }

    @Bean
    public Binding workorderEventsBinding() {
        return BindingBuilder.bind(workorderEventsQueue()).to(mesTopicExchange()).with(RK_WORKORDER);
    }

    @Bean
    public Binding inventoryEventsBinding() {
        return BindingBuilder.bind(inventoryEventsQueue()).to(mesTopicExchange()).with(RK_INVENTORY);
    }

    @Bean
    public Binding qualityEventsBinding() {
        return BindingBuilder.bind(qualityEventsQueue()).to(mesTopicExchange()).with(RK_QUALITY);
    }

    @Bean
    public Binding apsSyncDlqBinding() {
        return BindingBuilder.bind(apsSyncDlq()).to(mesDlxExchange()).with("dlq.aps.sync");
    }

    @Bean
    public Binding workorderDlqBinding() {
        return BindingBuilder.bind(workorderEventsDlq()).to(mesDlxExchange()).with("dlq.workorder");
    }

    // ==================== Serialization ====================

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // Publish failure — 上游若需重试可在此落盘补偿日志
            }
        });
        return template;
    }
}
