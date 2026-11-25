package com.demo.inventory;

import com.demo.testsupport.TestResultLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static com.demo.testsupport.TestHttpLogging.operation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ExtendWith(TestResultLogger.class)
class InventoryItemRepositoryTest {

    @Autowired
    private InventoryItemRepository repository;

    @Test
    void savesAndFindsItemById() {
        InventoryItem item = new InventoryItem("SKU-001", "Widget", 10, new BigDecimal("9.99"));
        InventoryItem saved = operation("repository.save", item, () -> repository.save(item));

        var found = operation("repository.findById", saved.getId(), () -> repository.findById(saved.getId()));

        assertTrue(found.isPresent());
        assertEquals("SKU-001", found.orElseThrow().getSku());
        assertEquals("Widget", found.orElseThrow().getName());
        assertEquals(10, found.orElseThrow().getQuantity());
        assertEquals(new BigDecimal("9.99"), found.orElseThrow().getPrice());
    }
}
