package com.demo.systemtest;

import com.demo.testsupport.HttpExchangeLogger;
import com.demo.testsupport.TestResultLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(TestResultLogger.class)
abstract class ApiSystemTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    protected final RestTemplate restTemplate;
    protected final String collectionUrl;
    private final Set<String> createdResourceUrls = new LinkedHashSet<>();

    protected ApiSystemTest(String baseUrlProperty, String defaultBaseUrl, String collectionPath) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(requestFactory));
        restTemplate.getInterceptors().add(new HttpExchangeLogger());
        collectionUrl = System.getProperty(baseUrlProperty, defaultBaseUrl)
                .replaceAll("/+$", "") + collectionPath;
    }

    protected JsonNode create(String payload) {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(collectionUrl, jsonRequest(payload), JsonNode.class);
        JsonNode body = response.getBody();
        // Track the record before assertions so a failing test still cleans up its own data.
        if (body != null && body.path("id").isIntegralNumber()) {
            createdResourceUrls.add(resourceUrl(body.path("id").asLong()));
        }
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(body).isNotNull();
        assertThat(body.path("id").isIntegralNumber()).isTrue();
        assertThat(body.path("id").asLong()).isPositive();
        assertThat(response.getHeaders().getLocation())
                .hasToString(resourceUrl(body.path("id").asLong()));
        return body;
    }

    protected String resourceUrl(long id) {
        return collectionUrl + "/" + id;
    }

    protected HttpEntity<String> jsonRequest(String payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(payload, headers);
    }

    protected JsonNode json(String payload) {
        try {
            return OBJECT_MAPPER.readTree(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid test JSON", exception);
        }
    }

    protected void assertJson(JsonNode actual, long id, JsonNode expected) {
        assertThat(actual).isNotNull();
        ObjectNode expectedWithId = expected.deepCopy();
        expectedWithId.put("id", id);
        try {
            JSONAssert.assertEquals(expectedWithId.toString(), actual.toString(), JSONCompareMode.STRICT);
        } catch (JSONException exception) {
            throw new AssertionError("Could not compare JSON", exception);
        }
    }

    protected void assertStatus(HttpStatus expected, HttpMethod method, String url, String payload) {
        HttpClientErrorException error = assertThrows(HttpClientErrorException.class,
                () -> restTemplate.exchange(url, method, jsonRequest(payload), String.class));
        assertThat(error.getStatusCode()).isEqualTo(expected);
    }

    @AfterEach
    void cleanUpCreatedResources() {
        RuntimeException cleanupFailure = null;
        for (String url : createdResourceUrls) {
            try {
                restTemplate.delete(url);
            } catch (HttpClientErrorException.NotFound ignored) {
                // A delete test may already have deleted this record.
            } catch (RuntimeException failure) {
                if (cleanupFailure == null) {
                    cleanupFailure = failure;
                } else {
                    cleanupFailure.addSuppressed(failure);
                }
            }
        }
        if (cleanupFailure != null) {
            throw cleanupFailure;
        }
    }
}
