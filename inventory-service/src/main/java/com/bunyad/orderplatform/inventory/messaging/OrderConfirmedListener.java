package com.bunyad.orderplatform.inventory.messaging;

import com.bunyad.orderplatform.inventory.application.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderConfirmedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmedListener.class);

    private final InventoryService inventoryService;
    private final JsonMapper jsonMapper;

    public OrderConfirmedListener(InventoryService inventoryService, JsonMapper jsonMapper) {
        this.inventoryService = inventoryService;
        this.jsonMapper = jsonMapper;
    }

    @RabbitListener(queues = MessagingConfig.ORDER_CONFIRMED_QUEUE)
    public void onOrderConfirmed(Message message) {
        OrderConfirmedEvent event = jsonMapper.readValue(message.getBody(), OrderConfirmedEvent.class);
        switch (inventoryService.reserveFor(event)) {
            case RESERVED -> log.info("Reserved stock for order {}", event.orderId());
            case REJECTED -> log.info("Stock reservation failed for order {}", event.orderId());
            case DUPLICATE -> log.info("Event {} already processed, skipping", event.eventId());
        }
    }
}
