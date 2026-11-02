package com.bunyad.orderplatform.order.api;

import com.bunyad.orderplatform.order.api.OrderDtos.OrderLineRequest;
import com.bunyad.orderplatform.order.api.OrderDtos.OrderResponse;
import com.bunyad.orderplatform.order.api.OrderDtos.PlaceOrderRequest;
import com.bunyad.orderplatform.order.application.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request) {
        var lines = request.lines().stream().map(OrderLineRequest::toDomain).toList();
        var order = orderService.placeOrder(request.customerId(), lines);
        return ResponseEntity.created(URI.create("/api/orders/" + order.getId()))
                .body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return OrderResponse.from(orderService.getOrder(id));
    }

    @GetMapping
    public List<OrderResponse> byCustomer(@RequestParam String customerId) {
        return orderService.getCustomerOrders(customerId).stream().map(OrderResponse::from).toList();
    }

    @PostMapping("/{id}/confirm")
    public OrderResponse confirm(@PathVariable UUID id) {
        return OrderResponse.from(orderService.confirmOrder(id));
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable UUID id) {
        return OrderResponse.from(orderService.cancelOrder(id));
    }
}
