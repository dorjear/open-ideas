package com.demo.systemtest;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryApiNegativeSystemTest extends ApiSystemTest {

    InventoryApiNegativeSystemTest() {
        super("inventory.base-url", "http://localhost:8082", "/api/items");
    }

    @Test
    void getMissingInventoryItem() {
        assertStatus(HttpStatus.NOT_FOUND, HttpMethod.GET, resourceUrl(-1), null);
    }

    @Test
    void updateMissingInventoryItem() {
        assertStatus(HttpStatus.NOT_FOUND, HttpMethod.PUT, resourceUrl(-1), validItem());
    }

    @Test
    void deleteMissingInventoryItem() {
        assertStatus(HttpStatus.NOT_FOUND, HttpMethod.DELETE, resourceUrl(-1), null);
    }

    @Test
    void createInvalidInventoryItem() {
        assertStatus(HttpStatus.BAD_REQUEST, HttpMethod.POST, collectionUrl, invalidItem());
    }

    @Test
    void updateInvalidInventoryItem() {
        String itemJson = validItem();
        JsonNode created = create(itemJson);
        long id = created.path("id").asLong();
        assertStatus(HttpStatus.BAD_REQUEST, HttpMethod.PUT, resourceUrl(id), invalidItem());
        assertThat(restTemplate.getForObject(resourceUrl(id), JsonNode.class)).isEqualTo(created);
    }

    private static String validItem() {
        return """
                {
                  "sku": "SYSTEM-%s",
                  "name": "Widget",
                  "quantity": 10,
                  "price": 9.99
                }
                """.formatted(UUID.randomUUID());
    }

    private static String invalidItem() {
        return """
                {
                  "sku": " ",
                  "name": "",
                  "quantity": -1,
                  "price": -0.01
                }
                """;
    }
}
