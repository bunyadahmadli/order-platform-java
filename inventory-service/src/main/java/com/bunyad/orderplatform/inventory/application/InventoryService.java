package com.bunyad.orderplatform.inventory.application;

import com.bunyad.orderplatform.inventory.domain.ProcessedEvent;
import com.bunyad.orderplatform.inventory.domain.ProcessedEventRepository;
import com.bunyad.orderplatform.inventory.domain.StockItem;
import com.bunyad.orderplatform.inventory.domain.StockItemRepository;
import com.bunyad.orderplatform.inventory.messaging.OrderConfirmedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final StockItemRepository stockItemRepository;
    private final ProcessedEventRepository processedEventRepository;

    public InventoryService(StockItemRepository stockItemRepository, ProcessedEventRepository processedEventRepository) {
        this.stockItemRepository = stockItemRepository;
        this.processedEventRepository = processedEventRepository;
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
    public boolean reserveFor(OrderConfirmedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            return false;
        }
        for (OrderConfirmedEvent.Line line : event.lines()) {
            StockItem item = stockItemRepository.findById(line.productCode())
                    .orElseThrow(() -> new StockNotFoundException(line.productCode()));
            item.decrease(line.quantity());
        }
        processedEventRepository.save(new ProcessedEvent(event.eventId()));
        return true;
    }
}
