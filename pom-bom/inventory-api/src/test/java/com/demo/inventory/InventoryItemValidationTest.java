package com.demo.inventory;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryItemValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsItemWithValidFields() {
        InventoryItem item = new InventoryItem("SKU-001", "Widget", 10, new BigDecimal("9.99"));

        assertTrue(validator.validate(item).isEmpty());
    }

    @Test
    void rejectsBlankNamesAndNegativeAmounts() {
        InventoryItem item = new InventoryItem(" ", "", -1, new BigDecimal("-0.01"));

        var violations = validator.validate(item);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("sku")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantity")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("price")));
    }
}
