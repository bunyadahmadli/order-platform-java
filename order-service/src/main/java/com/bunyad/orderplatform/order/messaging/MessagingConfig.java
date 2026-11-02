package com.bunyad.orderplatform.order.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class MessagingConfig {

    public static final String ORDER_EVENTS_EXCHANGE = "order.events";
    public static final String ORDER_CONFIRMED_ROUTING_KEY = "order.confirmed";

    @Bean
    TopicExchange orderEventsExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }
}
