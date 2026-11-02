package com.bunyad.orderplatform.order.outbox;

import com.bunyad.orderplatform.order.TestcontainersConfiguration;
import com.bunyad.orderplatform.order.application.OrderService;
import com.bunyad.orderplatform.order.domain.Order;
import com.bunyad.orderplatform.order.domain.OrderLine;
import com.bunyad.orderplatform.order.messaging.MessagingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "outbox.publish-interval=3600000")
@Import(TestcontainersConfiguration.class)
class OutboxIntegrationTest {

    private static final String TEST_QUEUE = "test.order-confirmed";

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private TopicExchange orderEventsExchange;

    @Test
    void confirmingOrderStoresEventAndPublishesIt() {
        Queue queue = new Queue(TEST_QUEUE, false, false, true);
        amqpAdmin.declareQueue(queue);
        Binding binding = BindingBuilder.bind(queue).to(orderEventsExchange).with(MessagingConfig.ORDER_CONFIRMED_ROUTING_KEY);
        amqpAdmin.declareBinding(binding);

        Order order = orderService.placeOrder("customer-7", List.of(new OrderLine("SKU-9", 3, BigDecimal.TEN)));
        orderService.confirmOrder(order.getId());

        List<OutboxEvent> events = outboxEventRepository.findByAggregateId(order.getId());
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getPublishedAt()).isNull();

        outboxPublisher.publishPending();

        assertThat(outboxEventRepository.findById(events.getFirst().getId()).orElseThrow().getPublishedAt()).isNotNull();

        Message message = rabbitTemplate.receive(TEST_QUEUE, 5000);
        assertThat(message).isNotNull();
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(events.getFirst().getId().toString());
        assertThat(new String(message.getBody(), StandardCharsets.UTF_8))
                .contains(order.getId().toString())
                .contains("\"productCode\":\"SKU-9\"")
                .contains("\"quantity\":3");
    }

    @Test
    void cancellingOrderDoesNotCreateEvent() {
        Order order = orderService.placeOrder("customer-8", List.of(new OrderLine("SKU-1", 1, BigDecimal.ONE)));

        orderService.cancelOrder(order.getId());

        assertThat(outboxEventRepository.findByAggregateId(order.getId())).isEmpty();
    }
}
