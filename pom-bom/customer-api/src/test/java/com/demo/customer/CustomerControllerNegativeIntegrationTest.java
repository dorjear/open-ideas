package com.demo.customer;

import com.demo.testsupport.TestResultLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.demo.testsupport.TestHttpLogging.perform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Starts the full application context: MockMvc -> controller -> real JPA repository -> H2.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:customer-controller-test",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ExtendWith(TestResultLogger.class)
class CustomerControllerNegativeIntegrationTest {

    private static final String VALID_CUSTOMER = """
            {
              "firstName": "Ada",
              "lastName": "Lovelace",
              "email": "ada@example.com"
            }
            """;
    private static final String INVALID_CUSTOMER = """
            {
              "firstName": " ",
              "lastName": "",
              "email": "not-an-email"
            }
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository repository;

    @MockitoBean
    private AgifyClient agifyClient;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void missingCustomerReturnsNotFound(String method) throws Exception {
        perform(mvc, request(HttpMethod.valueOf(method), "/api/customers/-1")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_CUSTOMER))
                .andExpect(status().isNotFound());

        assertEquals(0, repository.count());
        verifyNoInteractions(agifyClient);
    }

    @Test
    void invalidCreateDoesNotPersistCustomer() throws Exception {
        perform(mvc, post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON).content(INVALID_CUSTOMER))
                .andExpect(status().isBadRequest());

        assertEquals(0, repository.count());
        verifyNoInteractions(agifyClient);
    }

    @Test
    void invalidUpdateDoesNotChangePersistedCustomer() throws Exception {
        Customer saved = repository.save(objectMapper.readValue(VALID_CUSTOMER, Customer.class));
        String expectedCustomerJson = objectMapper.writeValueAsString(saved);

        perform(mvc, put("/api/customers/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(INVALID_CUSTOMER))
                .andExpect(status().isBadRequest());

        Customer unchanged = repository.findById(saved.getId()).orElseThrow();
        JSONAssert.assertEquals(expectedCustomerJson, objectMapper.writeValueAsString(unchanged), JSONCompareMode.STRICT);
        assertEquals(1, repository.count());
        verifyNoInteractions(agifyClient);
    }

    @Test
    void agifyFailurePreventsCreate() throws Exception {
        when(agifyClient.getAge("Ada")).thenThrow(new AgifyUnavailableException());
        perform(mvc, post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_CUSTOMER))
                .andExpect(status().isBadGateway());
        assertEquals(0, repository.count());
        verify(agifyClient).getAge("Ada");
    }

    @Test
    void agifyFailureLeavesCustomerUnchanged() throws Exception {
        Customer customer = objectMapper.readValue(VALID_CUSTOMER, Customer.class);
        customer.setAge(42);
        Customer saved = repository.save(customer);
        String expectedCustomerJson = objectMapper.writeValueAsString(saved);
        when(agifyClient.getAge("Grace")).thenThrow(new AgifyUnavailableException());
        String updatedCustomerJson = """
                {"firstName":"Grace","lastName":"Hopper","email":"grace@example.com"}
                """;
        perform(mvc, put("/api/customers/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(updatedCustomerJson))
                .andExpect(status().isBadGateway());
        Customer unchanged = repository.findById(saved.getId()).orElseThrow();
        JSONAssert.assertEquals(expectedCustomerJson, objectMapper.writeValueAsString(unchanged), JSONCompareMode.STRICT);
        perform(mvc, get("/api/customers/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedCustomerJson, org.springframework.test.json.JsonCompareMode.STRICT));
        assertEquals(1, repository.count());
        verify(agifyClient).getAge("Grace");
        verifyNoMoreInteractions(agifyClient);
    }
}
