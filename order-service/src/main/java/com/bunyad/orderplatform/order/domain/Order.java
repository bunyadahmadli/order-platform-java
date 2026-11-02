package com.bunyad.orderplatform.order.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_lines", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderLine> lines = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected Order() {
    }

    public static Order place(String customerId, List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("An order needs at least one line");
        }
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.customerId = customerId;
        order.status = OrderStatus.PENDING;
        order.lines.addAll(lines);
        order.createdAt = Instant.now();
        return order;
    }

    public BigDecimal totalAmount() {
        return lines.stream()
                .map(OrderLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed, current status: " + status);
        }
        status = OrderStatus.CONFIRMED;
    }

    public void cancel() {
        if (status == OrderStatus.CANCELLED) {
            return;
        }
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Order cannot be cancelled after it has been shipped, current status: " + status);
        }
        status = OrderStatus.CANCELLED;
    }

    public void ship() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed orders can be shipped, current status: " + status);
        }
        status = OrderStatus.SHIPPED;
    }

    public void markDelivered() {
        if (status != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Only shipped orders can be delivered, current status: " + status);
        }
        status = OrderStatus.DELIVERED;
    }

    public UUID getId() { return id; }
    public String getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public List<OrderLine> getLines() { return Collections.unmodifiableList(lines); }
    public Instant getCreatedAt() { return createdAt; }
}
