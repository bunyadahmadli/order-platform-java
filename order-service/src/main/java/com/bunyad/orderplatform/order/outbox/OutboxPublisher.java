package com.bunyad.orderplatform.order.outbox;

import com.bunyad.orderplatform.order.messaging.MessagingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.publish-interval}")
    @Transactional
    public void publishPending() {
        for (OutboxEvent event : outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                rabbitTemplate.send(MessagingConfig.ORDER_EVENTS_EXCHANGE, event.getRoutingKey(), toMessage(event));
                event.markPublished();
            } catch (Exception ex) {
                log.warn("Could not publish outbox event {}, will retry", event.getId(), ex);
                return;
            }
        }
    }

    private Message toMessage(OutboxEvent event) {
        return MessageBuilder.withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(event.getId().toString())
                .setType(event.getEventType())
                .build();
    }
}
