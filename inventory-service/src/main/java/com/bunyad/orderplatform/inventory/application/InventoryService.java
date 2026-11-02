package com.bunyad.orderplatform.inventory.application;

import com.bunyad.orderplatform.inventory.domain.ProcessedEvent;
import com.bunyad.orderplatform.inventory.domain.ProcessedEventRepository;
import com.bunyad.orderplatform.inventory.domain.StockItem;
import com.bunyad.orderplatform.inventory.domain.StockItemRepository;
import com.bunyad.orderplatform.inventory.messaging.MessagingConfig;
import com.bunyad.orderplatform.inventory.messaging.OrderConfirmedEvent;
import com.bunyad.orderplatform.inventory.messaging.StockReservationFailedEvent;
import com.bunyad.orderplatform.inventory.outbox.OutboxEvent;
import com.bunyad.orderplatform.inventory.outbox.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryService {

    private final StockItemRepository stockItemRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public InventoryService(StockItemRepository stockItemRepository, ProcessedEventRepository processedEventRepository,
                            OutboxEventRepository outboxEventRepository, JsonMapper jsonMapper) {
        this.stockItemRepository = stockItemRepository;
        this.processedEventRepository = processedEventRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public StockItem setStock(String productCode, int quantity) {
        StockItem item = stockItemRepository.findById(productCode)
                .orElseGet(() -> new StockItem(productCode, 0));
        item.setAvailableQuantity(quantity);
        return stockItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public StockItem getStock(String productCode) {
        return stockItemRepository.findById(productCode)
                .orElseThrow(() -> new StockNotFoundException(productCode));
    }

    @Transactional
    public ReservationResult reserveFor(OrderConfirmedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            return ReservationResult.DUPLICATE;
        }
        processedEventRepository.save(new ProcessedEvent(event.eventId()));

        Map<String, Integer> requested = new HashMap<>();
        event.lines().forEach(line -> requested.merge(line.productCode(), line.quantity(), Integer::sum));

        Map<String, StockItem> items = new HashMap<>();
        for (Map.Entry<String, Integer> entry : requested.entrySet()) {
            Optional<StockItem> item = stockItemRepository.findById(entry.getKey());
            if (item.isEmpty()) {
                return reject(event, "Unknown product " + entry.getKey());
            }
            if (item.get().getAvailableQuantity() < entry.getValue()) {
                return reject(event, "Insufficient stock for " + entry.getKey()
                        + ": requested " + entry.getValue() + ", available " + item.get().getAvailableQuantity());
            }
            items.put(entry.getKey(), item.get());
        }

        requested.forEach((productCode, quantity) -> items.get(productCode).decrease(quantity));
        return ReservationResult.RESERVED;
    }

    private ReservationResult reject(OrderConfirmedEvent event, String reason) {
        UUID eventId = UUID.randomUUID();
        String payload = jsonMapper.writeValueAsString(new StockReservationFailedEvent(eventId, event.orderId(), reason));
        outboxEventRepository.save(new OutboxEvent(eventId, event.orderId(), "StockReservationFailed",
                MessagingConfig.STOCK_RESERVATION_FAILED_ROUTING_KEY, payload));
        return ReservationResult.REJECTED;
    }
}
