package com.bunyad.orderplatform.inventory.messaging;

import com.bunyad.orderplatform.inventory.TestcontainersConfiguration;
import com.bunyad.orderplatform.inventory.application.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OrderConfirmedListenerIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private InventoryService inventoryService;

    @Test
    void decreasesStockWhenOrderIsConfirmed() {
        inventoryService.setStock("SKU-A", 10);

        publish(UUID.randomUUID(), "SKU-A", 3);

        awaitStock("SKU-A", 7);
    }

    @Test
    void ignoresDuplicateEvent() {
        inventoryService.setStock("SKU-B", 10);
        UUID eventId = UUID.randomUUID();

        publish(eventId, "SKU-B", 2);
        awaitStock("SKU-B", 8);

        publish(eventId, "SKU-B", 2);
        publish(UUID.randomUUID(), "SKU-B", 1);

        awaitStock("SKU-B", 7);
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(stockOf("SKU-B")).isEqualTo(7));
    }

    @Test
    void sendsEventToDeadLetterQueueWhenStockIsInsufficient() {
        inventoryService.setStock("SKU-C", 1);

        publish(UUID.randomUUID(), "SKU-C", 5);

        Message dead = rabbitTemplate.receive(MessagingConfig.ORDER_CONFIRMED_DLQ, 10_000);
        assertThat(dead).isNotNull();
        assertThat(inventoryService.getStock("SKU-C").getAvailableQuantity()).isEqualTo(1);
    }

    private void publish(UUID eventId, String productCode, int quantity) {
        String json = """
                {"eventId":"%s","orderId":"%s","lines":[{"productCode":"%s","quantity":%d}]}
                """.formatted(eventId, UUID.randomUUID(), productCode, quantity);
        Message message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(eventId.toString())
                .build();
        rabbitTemplate.send(MessagingConfig.ORDER_EVENTS_EXCHANGE, MessagingConfig.ORDER_CONFIRMED_ROUTING_KEY, message);
    }

    private void awaitStock(String productCode, int expected) {
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(stockOf(productCode)).isEqualTo(expected));
    }

    private int stockOf(String productCode) {
        return inventoryService.getStock(productCode).getAvailableQuantity();
    }
}
