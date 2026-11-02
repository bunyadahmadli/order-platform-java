package com.bunyad.orderplatform.order.messaging;

import com.bunyad.orderplatform.order.application.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StockReservationFailedListener {

    private static final Logger log = LoggerFactory.getLogger(StockReservationFailedListener.class);

    private final OrderService orderService;
    private final JsonMapper jsonMapper;

    public StockReservationFailedListener(OrderService orderService, JsonMapper jsonMapper) {
        this.orderService = orderService;
        this.jsonMapper = jsonMapper;
    }

    @RabbitListener(queues = MessagingConfig.STOCK_RESERVATION_FAILED_QUEUE)
    public void onStockReservationFailed(Message message) {
        StockReservationFailedEvent event = jsonMapper.readValue(message.getBody(), StockReservationFailedEvent.class);
        orderService.rejectOrder(event.orderId(), event.reason());
        log.info("Order {} rejected: {}", event.orderId(), event.reason());
    }
}
