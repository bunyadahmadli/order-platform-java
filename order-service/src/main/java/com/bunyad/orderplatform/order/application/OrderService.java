package com.bunyad.orderplatform.order.application;

import com.bunyad.orderplatform.order.domain.Order;
import com.bunyad.orderplatform.order.domain.OrderLine;
import com.bunyad.orderplatform.order.domain.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order placeOrder(String customerId, List<OrderLine> lines) {
        return orderRepository.save(Order.place(customerId, lines));
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Order> getCustomerOrders(String customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional
    public Order confirmOrder(UUID id) {
        Order order = getOrder(id);
        order.confirm();
        return order;
    }

    @Transactional
    public Order cancelOrder(UUID id) {
        Order order = getOrder(id);
        order.cancel();
        return order;
    }
}
