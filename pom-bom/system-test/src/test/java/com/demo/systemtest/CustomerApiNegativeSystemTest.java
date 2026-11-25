package com.demo.systemtest;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerApiNegativeSystemTest extends ApiSystemTest {

    CustomerApiNegativeSystemTest() {
        super("customer.base-url", "http://localhost:8081", "/api/customers");
    }

    @Test
    void getMissingCustomer() {
        // Generated IDs are positive, so this cannot target another user's record.
        assertStatus(HttpStatus.NOT_FOUND, HttpMethod.GET, resourceUrl(-1), null);
    }

    @Test
    void updateMissingCustomer() {
        assertStatus(HttpStatus.NOT_FOUND, HttpMethod.PUT, resourceUrl(-1), validCustomer());
    }

    @Test
    void deleteMissingCustomer() {
        assertStatus(HttpStatus.NOT_FOUND, HttpMethod.DELETE, resourceUrl(-1), null);
    }

    @Test
    void createInvalidCustomer() {
        assertStatus(HttpStatus.BAD_REQUEST, HttpMethod.POST, collectionUrl, invalidCustomer());
    }

    @Test
    void updateInvalidCustomer() {
        String customerJson = validCustomer();
        JsonNode created = create(customerJson);
        long id = created.path("id").asLong();
        assertStatus(HttpStatus.BAD_REQUEST, HttpMethod.PUT, resourceUrl(id), invalidCustomer());
        assertThat(restTemplate.getForObject(resourceUrl(id), JsonNode.class)).isEqualTo(created);
    }

    private static String validCustomer() {
        return """
                {
                  "firstName": "Ada",
                  "lastName": "Lovelace",
                  "email": "%s@example.com"
                }
                """.formatted(UUID.randomUUID());
    }

    private static String invalidCustomer() {
        return """
                {
                  "firstName": " ",
                  "lastName": "",
                  "email": "not-an-email"
                }
                """;
    }
}
