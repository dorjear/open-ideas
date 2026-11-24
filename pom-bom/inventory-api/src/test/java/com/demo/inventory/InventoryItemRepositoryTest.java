package com.demo.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class InventoryItemRepositoryTest {

    @Autowired
    private InventoryItemRepository repository;

    @Test
    void savesAndFindsItemById() {
        InventoryItem saved = repository.save(
                new InventoryItem("SKU-001", "Widget", 10, new BigDecimal("9.99"))
        );

        var found = repository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("SKU-001", found.orElseThrow().getSku());
        assertEquals("Widget", found.orElseThrow().getName());
        assertEquals(10, found.orElseThrow().getQuantity());
        assertEquals(new BigDecimal("9.99"), found.orElseThrow().getPrice());
    }
}
