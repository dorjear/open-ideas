package com.demo.systemtest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static com.demo.testsupport.TestResultLogger.step;
import static org.assertj.core.api.Assertions.assertThat;

class CustomerApiSystemTest extends ApiSystemTest {

    private static final Logger log = LoggerFactory.getLogger(CustomerApiSystemTest.class);

    private long customerId;
    private JsonNode expectedCustomer;

    CustomerApiSystemTest() {
        super("customer.base-url", "http://localhost:8081", "/api/customers");
    }

    @Test
    void customerLifecycle() {
        createCustomer();
        listCustomer();
        enquiryCustomer();
        updateCustomer();
        enquiryCustomer();
        deleteCustomer();
    }

    private void createCustomer() {
        step("createCustomer");
        String customerJson = """
                {
                  "firstName": "Ada",
                  "lastName": "Lovelace",
                  "email": "%s@example.com"
                }
                """.formatted(UUID.randomUUID());
        expectedCustomer = json(customerJson);
        JsonNode created = create(customerJson);
        customerId = created.path("id").asLong();
        includeGeneratedAge(expectedCustomer, created);
        assertJson(created, customerId, expectedCustomer);
    }

    private void listCustomer() {
        step("listCustomer");
        var listed = restTemplate.getForEntity(collectionUrl, JsonNode[].class);
        assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listed.getBody())
                .anySatisfy(customer -> assertJson(customer, customerId, expectedCustomer));
    }

    private void enquiryCustomer() {
        step("enquiryCustomer");
        var fetched = restTemplate.getForEntity(resourceUrl(customerId), JsonNode.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertJson(fetched.getBody(), customerId, expectedCustomer);
    }

    private void updateCustomer() {
        step("updateCustomer");
        waitBeforeUpdate();
        String customerJson = """
                {
                  "firstName": "Grace",
                  "lastName": "Hopper",
                  "email": "%s@example.com"
                }
                """.formatted(UUID.randomUUID());
        JsonNode updated = json(customerJson);
        var response = restTemplate.exchange(resourceUrl(customerId), HttpMethod.PUT,
                jsonRequest(customerJson), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        includeGeneratedAge(updated, response.getBody());
        assertJson(response.getBody(), customerId, updated);
        expectedCustomer = updated;
    }

    private void waitBeforeUpdate() {
        log.info("Waiting {} ms before updateCustomer to reduce Agify request throttling",
                TestConfig.CUSTOMER_UPDATE_DELAY.toMillis());
        try {
            Thread.sleep(TestConfig.CUSTOMER_UPDATE_DELAY);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting before customer update", exception);
        }
    }

    private void deleteCustomer() {
        step("deleteCustomer");
        String url = resourceUrl(customerId);
        var deleted = restTemplate.exchange(url, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(deleted.getBody()).isNull();
        assertStatus(HttpStatus.NOT_FOUND, HttpMethod.GET, url, null);
        assertThat(restTemplate.getForObject(collectionUrl, JsonNode[].class))
                .noneMatch(customer -> customer.path("id").asLong() == customerId);
    }

    private void includeGeneratedAge(JsonNode expected, JsonNode actual) {
        assertThat(actual).isNotNull();
        assertThat(actual.has("age")).isTrue();
        JsonNode age = actual.get("age");
        assertThat(age.isNull() || (age.isIntegralNumber() && age.asInt() >= 0)).isTrue();
        ((ObjectNode) expected).set("age", age);
    }
}
