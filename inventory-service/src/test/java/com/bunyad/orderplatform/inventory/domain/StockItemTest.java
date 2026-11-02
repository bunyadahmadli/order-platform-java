package com.bunyad.orderplatform.inventory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockItemTest {

    @Test
    void decreasesAvailableQuantity() {
        StockItem item = new StockItem("SKU-1", 10);

        item.decrease(4);

        assertThat(item.getAvailableQuantity()).isEqualTo(6);
    }

    @Test
    void rejectsDecreaseBeyondAvailableStock() {
        StockItem item = new StockItem("SKU-1", 2);

        assertThatThrownBy(() -> item.decrease(3)).isInstanceOf(InsufficientStockException.class);
        assertThat(item.getAvailableQuantity()).isEqualTo(2);
    }

    @Test
    void rejectsNegativeStock() {
        assertThatThrownBy(() -> new StockItem("SKU-1", -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
