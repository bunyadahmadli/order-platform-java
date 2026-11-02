package com.bunyad.orderplatform.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "stock_items")
public class StockItem {

    @Id
    private String productCode;

    @Column(nullable = false)
    private int availableQuantity;

    @Version
    private long version;

    protected StockItem() {
    }

    public StockItem(String productCode, int availableQuantity) {
        this.productCode = productCode;
        setAvailableQuantity(availableQuantity);
    }

    public void setAvailableQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        this.availableQuantity = quantity;
    }

    public void decrease(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (quantity > availableQuantity) {
            throw new InsufficientStockException(productCode, quantity, availableQuantity);
        }
        availableQuantity -= quantity;
    }

    public String getProductCode() { return productCode; }
    public int getAvailableQuantity() { return availableQuantity; }
}
