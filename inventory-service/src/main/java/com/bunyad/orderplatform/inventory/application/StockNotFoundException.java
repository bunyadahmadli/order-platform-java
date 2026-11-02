package com.bunyad.orderplatform.inventory.application;

public class StockNotFoundException extends RuntimeException {

    public StockNotFoundException(String productCode) {
        super("No stock record for product " + productCode);
    }
}
