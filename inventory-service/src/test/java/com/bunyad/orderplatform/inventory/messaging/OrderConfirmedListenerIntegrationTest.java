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
import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OrderConfirmedListenerIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private InventoryService inventoryService;

    @Test
    void decreasesStockWhenOrderIsConfirmed() throws InterruptedException {
        inventoryService.setStock("SKU-A", 10);

        publish(UUID.randomUUID(), "SKU-A", 3);

        waitUntil(() -> inventoryService.getStock("SKU-A").getAvailableQuantity() == 7);
    }

    @Test
    void ignoresDuplicateEvent() throws InterruptedException {
        inventoryService.setStock("SKU-B", 10);
        UUID eventId = UUID.randomUUID();

        publish(eventId, "SKU-B", 2);
        waitUntil(() -> inventoryService.getStock("SKU-B").getAvailableQuantity() == 8);

        publish(eventId, "SKU-B", 2);
        publish(UUID.randomUUID(), "SKU-B", 1);

        waitUntil(() -> inventoryService.getStock("SKU-B").getAvailableQuantity() == 7);
        Thread.sleep(500);
        assertThat(inventoryService.getStock("SKU-B").getAvailableQuantity()).isEqualTo(7);
    }

    @Test
    void sendsEventToDeadLetterQueueWhenStockIsInsufficient() throws InterruptedException {
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

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Condition not met within 10 seconds");
    }
}
