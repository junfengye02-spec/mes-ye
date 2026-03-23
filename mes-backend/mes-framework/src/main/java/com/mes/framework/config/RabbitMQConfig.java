package com.mes.framework.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public TopicExchange mesTopicExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_MES_TOPIC).durable(true).build();
    }

    @Bean
    public TopicExchange mesDlxExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_MES_DLX).durable(true).build();
    }

    // ==================== Queues ====================

    @Bean
    public Queue apsSyncQueue() {
        return QueueBuilder.durable(QUEUE_APS_SYNC)
                .withArgument("x-dead-letter-exchange", EXCHANGE_MES_DLX)
                .withArgument("x-dead-letter-routing-key", "dlq.aps.sync")
                .build();
    }

    @Bean
    public Queue workorderEventsQueue() {
        return QueueBuilder.durable(QUEUE_WORKORDER_EVENTS)
                .withArgument("x-dead-letter-exchange", EXCHANGE_MES_DLX)
                .withArgument("x-dead-letter-routing-key", "dlq.workorder")
                .build();
    }

    @Bean
    public Queue inventoryEventsQueue() {
        return QueueBuilder.durable(QUEUE_INVENTORY_EVENTS).build();
    }

    @Bean
    public Queue qualityEventsQueue() {
        return QueueBuilder.durable(QUEUE_QUALITY_EVENTS).build();
    }

    @Bean
    public Queue apsSyncDlq() {
        return QueueBuilder.durable(QUEUE_APS_SYNC_DLQ).build();
    }

    @Bean
    public Queue workorderEventsDlq() {
        return QueueBuilder.durable(QUEUE_WORKORDER_EVENTS_DLQ).build();
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
                // Log publish failure for retry
            }
        });
        return template;
    }
}
