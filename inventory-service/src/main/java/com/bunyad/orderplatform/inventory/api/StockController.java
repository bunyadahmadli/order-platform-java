package com.bunyad.orderplatform.inventory.api;

import com.bunyad.orderplatform.inventory.application.InventoryService;
import com.bunyad.orderplatform.inventory.domain.StockItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final InventoryService inventoryService;

    public StockController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productCode}")
    public StockResponse get(@PathVariable String productCode) {
        return StockResponse.from(inventoryService.getStock(productCode));
    }

    @PutMapping("/{productCode}")
    public StockResponse set(@PathVariable String productCode, @Valid @RequestBody SetStockRequest request) {
        return StockResponse.from(inventoryService.setStock(productCode, request.quantity()));
    }

    public record SetStockRequest(@PositiveOrZero int quantity) {
    }

    public record StockResponse(String productCode, int availableQuantity) {

        static StockResponse from(StockItem item) {
            return new StockResponse(item.getProductCode(), item.getAvailableQuantity());
        }
    }
}
