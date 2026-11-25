package com.demo.systemtest;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static com.demo.testsupport.TestResultLogger.step;
import static org.assertj.core.api.Assertions.assertThat;

class InventoryApiSystemTest extends ApiSystemTest {

    private long itemId;
    private JsonNode expectedItem;

    InventoryApiSystemTest() {
        super("inventory.base-url", "http://localhost:8082", "/api/items");
    }

    @Test
    void inventoryItemLifecycle() {
        createInventoryItem();
        listInventoryItem();
        enquiryInventoryItem();
        updateInventoryItem();
        enquiryInventoryItem();
        deleteInventoryItem();
    }

    private void createInventoryItem() {
        step("createInventoryItem");
        String itemJson = """
                {
                  "sku": "SYSTEM-%s",
                  "name": "Widget",
                  "quantity": 10,
                  "price": 9.99
                }
                """.formatted(UUID.randomUUID());
        expectedItem = json(itemJson);
        JsonNode created = create(itemJson);
        itemId = created.path("id").asLong();
        assertJson(created, itemId, expectedItem);
    }

    private void listInventoryItem() {
        step("listInventoryItem");
        var listed = restTemplate.getForEntity(collectionUrl, JsonNode[].class);
        assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listed.getBody()).anySatisfy(item -> assertJson(item, itemId, expectedItem));
    }

    private void enquiryInventoryItem() {
        step("enquiryInventoryItem");
        var fetched = restTemplate.getForEntity(resourceUrl(itemId), JsonNode.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertJson(fetched.getBody(), itemId, expectedItem);
    }

    private void updateInventoryItem() {
        step("updateInventoryItem");
        String itemJson = """
                {
                  "sku": "SYSTEM-%s",
                  "name": "Updated widget",
                  "quantity": 12,
                  "price": 19.95
                }
                """.formatted(UUID.randomUUID());
        JsonNode updated = json(itemJson);
        var response = restTemplate.exchange(resourceUrl(itemId), HttpMethod.PUT,
                jsonRequest(itemJson), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertJson(response.getBody(), itemId, updated);
        expectedItem = updated;
    }

    private void deleteInventoryItem() {
        step("deleteInventoryItem");
        String url = resourceUrl(itemId);
        var deleted = restTemplate.exchange(url, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(deleted.getBody()).isNull();
        assertStatus(HttpStatus.NOT_FOUND, HttpMethod.GET, url, null);
        assertThat(restTemplate.getForObject(collectionUrl, JsonNode[].class))
                .noneMatch(item -> item.path("id").asLong() == itemId);
    }
}
