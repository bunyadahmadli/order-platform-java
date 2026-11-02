package com.bunyad.orderplatform.order.messaging;

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
    public static final String INVENTORY_EVENTS_EXCHANGE = "inventory.events";
    public static final String STOCK_RESERVATION_FAILED_ROUTING_KEY = "stock.reservation-failed";
    public static final String STOCK_RESERVATION_FAILED_QUEUE = "order.stock-reservation-failed";
    public static final String STOCK_RESERVATION_FAILED_DLQ = "order.stock-reservation-failed.dlq";

    @Bean
    TopicExchange orderEventsExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange inventoryEventsExchange() {
        return new TopicExchange(INVENTORY_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    Queue stockReservationFailedQueue() {
        return QueueBuilder.durable(STOCK_RESERVATION_FAILED_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(STOCK_RESERVATION_FAILED_DLQ)
                .build();
    }

    @Bean
    Queue stockReservationFailedDeadLetterQueue() {
        return QueueBuilder.durable(STOCK_RESERVATION_FAILED_DLQ).build();
    }

    @Bean
    Binding stockReservationFailedBinding(Queue stockReservationFailedQueue, TopicExchange inventoryEventsExchange) {
        return BindingBuilder.bind(stockReservationFailedQueue).to(inventoryEventsExchange)
                .with(STOCK_RESERVATION_FAILED_ROUTING_KEY);
    }
}
