package com.bunyad.orderplatform.order.application;

import com.bunyad.orderplatform.order.domain.Order;
import com.bunyad.orderplatform.order.domain.OrderLine;
import com.bunyad.orderplatform.order.domain.OrderRepository;
import com.bunyad.orderplatform.order.messaging.MessagingConfig;
import com.bunyad.orderplatform.order.messaging.OrderConfirmedEvent;
import com.bunyad.orderplatform.order.outbox.OutboxEvent;
import com.bunyad.orderplatform.order.outbox.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public OrderService(OrderRepository orderRepository, OutboxEventRepository outboxEventRepository, JsonMapper jsonMapper) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public Order placeOrder(String customerId, List<OrderLine> lines) {
        return orderRepository.save(Order.place(customerId, lines));
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID id) {
        return findOrder(id);
    }

    @Transactional(readOnly = true)
    public List<Order> getCustomerOrders(String customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional
    public Order confirmOrder(UUID id) {
        Order order = findOrder(id);
        order.confirm();
        outboxEventRepository.save(orderConfirmedEvent(order));
        return order;
    }

    @Transactional
    public Order shipOrder(UUID id) {
        Order order = findOrder(id);
        order.ship();
        return order;
    }

    @Transactional
    public Order cancelOrder(UUID id) {
        Order order = findOrder(id);
        order.cancel();
        return order;
    }

    private Order findOrder(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    private OutboxEvent orderConfirmedEvent(Order order) {
        UUID eventId = UUID.randomUUID();
        List<OrderConfirmedEvent.Line> lines = order.getLines().stream()
                .map(l -> new OrderConfirmedEvent.Line(l.getProductCode(), l.getQuantity()))
                .toList();
        String payload = jsonMapper.writeValueAsString(new OrderConfirmedEvent(eventId, order.getId(), lines));
        return new OutboxEvent(eventId, order.getId(), "OrderConfirmed", MessagingConfig.ORDER_CONFIRMED_ROUTING_KEY, payload);
    }
}
