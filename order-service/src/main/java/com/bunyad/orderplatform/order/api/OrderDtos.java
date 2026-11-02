package com.bunyad.orderplatform.order.api;

import com.bunyad.orderplatform.order.domain.Order;
import com.bunyad.orderplatform.order.domain.OrderLine;
import com.bunyad.orderplatform.order.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record PlaceOrderRequest(
            @NotBlank String customerId,
            @NotEmpty List<@Valid OrderLineRequest> lines) {
    }

    public record OrderLineRequest(
            @NotBlank String productCode,
            @Positive int quantity,
            @NotNull @PositiveOrZero BigDecimal unitPrice) {

        OrderLine toDomain() {
            return new OrderLine(productCode, quantity, unitPrice);
        }
    }

    public record OrderLineResponse(String productCode, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    public record OrderResponse(
            UUID id,
            String customerId,
            OrderStatus status,
            List<OrderLineResponse> lines,
            BigDecimal totalAmount,
            Instant createdAt) {

        static OrderResponse from(Order order) {
            List<OrderLineResponse> lines = order.getLines().stream()
                    .map(l -> new OrderLineResponse(l.getProductCode(), l.getQuantity(), l.getUnitPrice(), l.lineTotal()))
                    .toList();
            return new OrderResponse(order.getId(), order.getCustomerId(), order.getStatus(),
                    lines, order.totalAmount(), order.getCreatedAt());
        }
    }
}
