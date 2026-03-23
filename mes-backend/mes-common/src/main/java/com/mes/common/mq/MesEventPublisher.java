package com.mes.common.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MesEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(String routingKey, Object payload) {
        try {
            String eventId = UUID.randomUUID().toString();
            MesEventWrapper wrapper = new MesEventWrapper(
                    eventId, routingKey, payload, LocalDateTime.now());

            rabbitTemplate.convertAndSend(MesEvents.EXCHANGE, routingKey, wrapper);
            log.debug("Published event: routingKey={}, eventId={}", routingKey, eventId);
        } catch (Exception e) {
            log.error("Failed to publish event: routingKey={}, error={}", routingKey, e.getMessage(), e);
            throw new RuntimeException("Event publish failed: " + routingKey, e);
        }
    }

    public void publishWithHeaders(String routingKey, Object payload, Map<String, Object> headers) {
        try {
            String eventId = UUID.randomUUID().toString();
            MesEventWrapper wrapper = new MesEventWrapper(
                    eventId, routingKey, payload, LocalDateTime.now());

            rabbitTemplate.convertAndSend(MesEvents.EXCHANGE, routingKey, wrapper, message -> {
                headers.forEach((k, v) -> message.getMessageProperties().setHeader(k, v));
                message.getMessageProperties().setMessageId(eventId);
                return message;
            });
            log.debug("Published event with headers: routingKey={}, eventId={}", routingKey, eventId);
        } catch (Exception e) {
            log.error("Failed to publish event: routingKey={}, error={}", routingKey, e.getMessage(), e);
            throw new RuntimeException("Event publish failed: " + routingKey, e);
        }
    }

    public record MesEventWrapper(
            String eventId,
            String eventType,
            Object payload,
            LocalDateTime timestamp
    ) {}
}
