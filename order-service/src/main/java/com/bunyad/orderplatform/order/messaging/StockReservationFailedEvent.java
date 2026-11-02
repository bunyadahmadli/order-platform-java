package com.bunyad.orderplatform.order.messaging;

import java.util.UUID;

public record StockReservationFailedEvent(UUID eventId, UUID orderId, String reason) {
}
