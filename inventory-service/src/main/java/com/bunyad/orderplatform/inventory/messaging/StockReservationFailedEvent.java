package com.bunyad.orderplatform.inventory.messaging;

import java.util.UUID;

public record StockReservationFailedEvent(UUID eventId, UUID orderId, String reason) {
}
