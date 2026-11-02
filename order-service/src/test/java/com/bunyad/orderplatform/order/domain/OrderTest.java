package com.bunyad.orderplatform.order.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    void placesOrderAsPendingAndCalculatesTotal() {
        Order order = Order.place("customer-1", List.of(
                new OrderLine("SKU-1", 2, new BigDecimal("10.50")),
                new OrderLine("SKU-2", 1, new BigDecimal("5.00"))));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.totalAmount()).isEqualByComparingTo("26.00");
    }

    @Test
    void rejectsOrderWithoutLines() {
        List<OrderLine> noLines = List.of();

        assertThatThrownBy(() -> Order.place("customer-1", noLines))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> new OrderLine("SKU-1", 0, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void confirmsPendingOrder() {
        Order order = newOrder();

        order.confirm();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void doesNotConfirmTwice() {
        Order order = newOrder();
        order.confirm();

        assertThatThrownBy(order::confirm).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PENDING", "CONFIRMED"})
    void cancelsOrderBeforeShipping(OrderStatus status) {
        Order order = orderWithStatus(status);

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"SHIPPED", "DELIVERED"})
    void doesNotCancelShippedOrder(OrderStatus status) {
        Order order = orderWithStatus(status);

        assertThatThrownBy(order::cancel).isInstanceOf(IllegalStateException.class);
        assertThat(order.getStatus()).isEqualTo(status);
    }

    @Test
    void shipsOnlyConfirmedOrder() {
        Order order = newOrder();

        assertThatThrownBy(order::ship).isInstanceOf(IllegalStateException.class);

        order.confirm();
        order.ship();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void cancellingTwiceIsIdempotent() {
        Order order = newOrder();
        order.cancel();

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    private static Order newOrder() {
        return Order.place("customer-1", List.of(new OrderLine("SKU-1", 1, BigDecimal.TEN)));
    }

    private static Order orderWithStatus(OrderStatus status) {
        Order order = newOrder();
        if (status == OrderStatus.PENDING) {
            return order;
        }
        order.confirm();
        if (status == OrderStatus.CONFIRMED) {
            return order;
        }
        order.ship();
        if (status == OrderStatus.DELIVERED) {
            order.markDelivered();
        }
        return order;
    }
}
