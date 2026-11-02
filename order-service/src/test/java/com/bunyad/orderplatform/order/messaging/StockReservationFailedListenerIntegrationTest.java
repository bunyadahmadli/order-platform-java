package com.bunyad.orderplatform.order.messaging;

import com.bunyad.orderplatform.order.TestcontainersConfiguration;
import com.bunyad.orderplatform.order.application.OrderService;
import com.bunyad.orderplatform.order.domain.Order;
import com.bunyad.orderplatform.order.domain.OrderLine;
import com.bunyad.orderplatform.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class StockReservationFailedListenerIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void rejectsOrderWhenStockReservationFails() {
        Order order = orderService.placeOrder("customer-9", List.of(new OrderLine("SKU-X", 5, BigDecimal.ONE)));
        orderService.confirmOrder(order.getId());

        publishFailure(order.getId(), "Insufficient stock for SKU-X");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Order rejected = orderService.getOrder(order.getId());
            assertThat(rejected.getStatus()).isEqualTo(OrderStatus.REJECTED);
            assertThat(rejected.getRejectionReason()).isEqualTo("Insufficient stock for SKU-X");
        });
    }

    @Test
    void sendsEventForUnknownOrderToDeadLetterQueue() {
        publishFailure(UUID.randomUUID(), "Unknown product SKU-Y");

        Message dead = rabbitTemplate.receive(MessagingConfig.STOCK_RESERVATION_FAILED_DLQ, 10_000);
        assertThat(dead).isNotNull();
    }

    private void publishFailure(UUID orderId, String reason) {
        UUID eventId = UUID.randomUUID();
        String json = """
                {"eventId":"%s","orderId":"%s","reason":"%s"}
                """.formatted(eventId, orderId, reason);
        Message message = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(eventId.toString())
                .build();
        rabbitTemplate.send(MessagingConfig.INVENTORY_EVENTS_EXCHANGE, MessagingConfig.STOCK_RESERVATION_FAILED_ROUTING_KEY, message);
    }
}
