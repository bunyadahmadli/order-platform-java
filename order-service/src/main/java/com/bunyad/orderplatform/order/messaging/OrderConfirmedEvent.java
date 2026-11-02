package com.bunyad.orderplatform.order.messaging;

import java.util.List;
import java.util.UUID;

public record OrderConfirmedEvent(UUID eventId, UUID orderId, List<Line> lines) {

    public record Line(String productCode, int quantity) {
    }
}
