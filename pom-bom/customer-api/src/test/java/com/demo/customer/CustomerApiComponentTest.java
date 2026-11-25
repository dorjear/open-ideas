package com.demo.customer;

import com.demo.testsupport.HttpExchangeLogger;
import com.demo.testsupport.TestResultLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;

import static com.demo.testsupport.TestHttpLogging.perform;
import static com.demo.testsupport.TestResultLogger.step;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Full Spring application with real Agify HTTP calls to WireMock and real persistence in H2.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:customer-component-test",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ExtendWith(TestResultLogger.class)
@Import(CustomerApiComponentTest.HttpLoggingConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CustomerApiComponentTest {

    @RegisterExtension
    static final WireMockExtension AGIFY = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().bindAddress("127.0.0.1"))
            .build();

    @DynamicPropertySource
    static void agifyProperties(DynamicPropertyRegistry properties) {
        properties.add("agify.base-url", AGIFY::baseUrl);
        properties.add("agify.api-key", () -> "component-test-key");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class HttpLoggingConfiguration {
        @Bean
        RestTemplateCustomizer agifyHttpLogging() {
            return new HttpExchangeLogger();
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository repository;

    private int expectedAgifyRequests;
    private long customerId;
    private String expectedCustomerJson;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
        expectedAgifyRequests = 0;
        AGIFY.stubFor(WireMock.get(WireMock.urlPathEqualTo("/"))
                .withQueryParam("name", WireMock.equalTo("Ada"))
                .withQueryParam("apikey", WireMock.equalTo("component-test-key"))
                .willReturn(WireMock.okJson("""
                        {"name":"Ada","age":42,"count":1234}
                        """)));
        AGIFY.stubFor(WireMock.get(WireMock.urlPathEqualTo("/"))
                .withQueryParam("name", WireMock.equalTo("Grace"))
                .withQueryParam("apikey", WireMock.equalTo("component-test-key"))
                .willReturn(WireMock.okJson("""
                        {"name":"Grace","age":55,"count":5678}
                        """)));
    }

    @Test
    void customerLifecycle() throws Exception {
        createCustomer();
        listCustomer();
        enquiryCustomer();
        updateCustomer();
        enquiryCustomer();
        deleteCustomer();
    }

    @Test
    void customerRequestContractContainsOnlyInputFields() throws Exception {
        var response = perform(mvc, get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse();
        var api = objectMapper.readTree(response.getContentAsString());
        var requestProperties = new HashSet<String>();
        api.at("/components/schemas/CustomerRequest/properties")
                .fieldNames().forEachRemaining(requestProperties::add);
        assertEquals(Set.of("firstName", "lastName", "email"), requestProperties);
        for (String operation : new String[]{
                "/paths/~1api~1customers/post",
                "/paths/~1api~1customers~1{id}/put"}) {
            assertEquals("#/components/schemas/CustomerRequest",
                    api.at(operation + "/requestBody/content/application~1json/schema/$ref").asText());
        }
        AGIFY.verify(0, WireMock.anyRequestedFor(WireMock.anyUrl()));
    }

    private void createCustomer() throws Exception {
        step("createCustomer");
        String customerJson = """
                {
                  "firstName": "Ada",
                  "lastName": "Lovelace",
                  "email": "ada@example.com"
                }
                """;
        var created = perform(mvc, post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON).content(customerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        customerId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        assertTrue(customerId > 0);
        expectedCustomerJson = withGeneratedFields(customerJson, 42);
        JSONAssert.assertEquals(expectedCustomerJson, created.getResponse().getContentAsString(), JSONCompareMode.STRICT);
        assertEquals("http://localhost/api/customers/" + customerId, created.getResponse().getHeader("Location"));
        assertPersistedCustomer();
        expectedAgifyRequests++;
        AGIFY.verify(1, WireMock.getRequestedFor(WireMock.urlPathEqualTo("/"))
                .withQueryParam("name", WireMock.equalTo("Ada")));
        assertAgifyRequestCount();
    }

    private void listCustomer() throws Exception {
        step("listCustomer");
        perform(mvc, get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(content().json("[" + expectedCustomerJson + "]", JsonCompareMode.STRICT));
        assertAgifyRequestCount();
    }

    private void enquiryCustomer() throws Exception {
        step("enquiryCustomer");
        perform(mvc, get("/api/customers/{id}", customerId))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedCustomerJson, JsonCompareMode.STRICT));
        assertAgifyRequestCount();
    }

    private void updateCustomer() throws Exception {
        step("updateCustomer");
        String customerJson = """
                {
                  "firstName": "Grace",
                  "lastName": "Hopper",
                  "email": "grace@example.com"
                }
                """;
        expectedCustomerJson = withGeneratedFields(customerJson, 55);
        perform(mvc, put("/api/customers/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON).content(customerJson))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedCustomerJson, JsonCompareMode.STRICT));
        assertPersistedCustomer();
        expectedAgifyRequests++;
        AGIFY.verify(1, WireMock.getRequestedFor(WireMock.urlPathEqualTo("/"))
                .withQueryParam("name", WireMock.equalTo("Grace")));
        assertAgifyRequestCount();
    }

    private void deleteCustomer() throws Exception {
        step("deleteCustomer");
        perform(mvc, delete("/api/customers/{id}", customerId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertFalse(repository.existsById(customerId));
        perform(mvc, get("/api/customers/{id}", customerId)).andExpect(status().isNotFound());
        perform(mvc, get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", JsonCompareMode.STRICT));
        assertAgifyRequestCount();
    }

    private void assertAgifyRequestCount() {
        AGIFY.verify(expectedAgifyRequests, WireMock.anyRequestedFor(WireMock.anyUrl()));
    }

    private String withGeneratedFields(String customerJson, int age) throws Exception {
        ObjectNode expected = (ObjectNode) objectMapper.readTree(customerJson);
        expected.put("id", customerId);
        expected.put("age", age);
        return expected.toString();
    }

    private void assertPersistedCustomer() throws Exception {
        Customer saved = repository.findById(customerId).orElseThrow();
        JSONAssert.assertEquals(expectedCustomerJson, objectMapper.writeValueAsString(saved), JSONCompareMode.STRICT);
        assertEquals(1, repository.count());
    }
}
