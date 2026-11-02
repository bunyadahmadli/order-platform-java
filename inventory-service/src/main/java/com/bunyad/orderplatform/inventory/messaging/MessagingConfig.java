package com.bunyad.orderplatform.inventory.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class MessagingConfig {

    public static final String ORDER_EVENTS_EXCHANGE = "order.events";
    public static final String ORDER_CONFIRMED_ROUTING_KEY = "order.confirmed";
    public static final String ORDER_CONFIRMED_QUEUE = "inventory.order-confirmed";
    public static final String ORDER_CONFIRMED_DLQ = "inventory.order-confirmed.dlq";
    public static final String INVENTORY_EVENTS_EXCHANGE = "inventory.events";
    public static final String STOCK_RESERVATION_FAILED_ROUTING_KEY = "stock.reservation-failed";

    @Bean
    TopicExchange orderEventsExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange inventoryEventsExchange() {
        return new TopicExchange(INVENTORY_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    Queue orderConfirmedQueue() {
        return QueueBuilder.durable(ORDER_CONFIRMED_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(ORDER_CONFIRMED_DLQ)
                .build();
    }

    @Bean
    Queue orderConfirmedDeadLetterQueue() {
        return QueueBuilder.durable(ORDER_CONFIRMED_DLQ).build();
    }

    @Bean
    Binding orderConfirmedBinding(Queue orderConfirmedQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderConfirmedQueue).to(orderEventsExchange).with(ORDER_CONFIRMED_ROUTING_KEY);
    }
}
