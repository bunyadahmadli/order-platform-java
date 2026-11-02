package com.bunyad.orderplatform.inventory.domain;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productCode, int requested, int available) {
        super("Insufficient stock for " + productCode + ": requested " + requested + ", available " + available);
    }
}
