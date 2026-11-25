package com.demo.customer;

import com.demo.testsupport.TestResultLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static com.demo.testsupport.TestHttpLogging.perform;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// MVC routes and JSON with a mocked service; the service owns persistence and enrichment.
@WebMvcTest(CustomerController.class)
@ExtendWith(TestResultLogger.class)
class CustomerControllerMvcTest {

    private static final String CUSTOMER_JSON = """
            {
              "firstName": "Ada",
              "lastName": "Lovelace",
              "email": "ada@example.com"
            }
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CustomerService service;

    @Test
    void listCustomer() throws Exception {
        when(service.getAll()).thenReturn(List.of(customer(7L)));
        perform(mvc, get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[" + expectedCustomer(7L) + "]", JsonCompareMode.STRICT));
    }

    @Test
    void enquiryCustomer() throws Exception {
        when(service.getById(7L)).thenReturn(Optional.of(customer(7L)));
        perform(mvc, get("/api/customers/7"))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedCustomer(7L), JsonCompareMode.STRICT));
    }

    @Test
    void getMissingCustomer() throws Exception {
        when(service.getById(7L)).thenReturn(Optional.empty());
        perform(mvc, get("/api/customers/7")).andExpect(status().isNotFound());
    }

    @Test
    void createCustomer() throws Exception {
        when(service.create(any(CustomerRequest.class))).thenReturn(customer(9L));
        perform(mvc, post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(CUSTOMER_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/customers/9"))
                .andExpect(content().json(expectedCustomer(9L), JsonCompareMode.STRICT));
        verify(service).create(new CustomerRequest("Ada", "Lovelace", "ada@example.com"));
    }

    @Test
    void updateCustomer() throws Exception {
        when(service.update(eq(7L), any(CustomerRequest.class))).thenReturn(Optional.of(customer(7L)));
        perform(mvc, put("/api/customers/7").contentType(MediaType.APPLICATION_JSON).content(CUSTOMER_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedCustomer(7L), JsonCompareMode.STRICT));
        verify(service).update(7L, new CustomerRequest("Ada", "Lovelace", "ada@example.com"));
    }

    @Test
    void updateMissingCustomer() throws Exception {
        when(service.update(eq(7L), any(CustomerRequest.class))).thenReturn(Optional.empty());
        perform(mvc, put("/api/customers/7").contentType(MediaType.APPLICATION_JSON).content(CUSTOMER_JSON))
                .andExpect(status().isNotFound());
        verify(service).update(eq(7L), any(CustomerRequest.class));
    }

    @Test
    void deleteCustomer() throws Exception {
        when(service.delete(7L)).thenReturn(true);
        perform(mvc, delete("/api/customers/7"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).delete(7L);
    }

    @Test
    void deleteMissingCustomer() throws Exception {
        when(service.delete(7L)).thenReturn(false);
        perform(mvc, delete("/api/customers/7")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void invalidCustomerDoesNotCallService(String method) throws Exception {
        String path = method.equals("POST") ? "/api/customers" : "/api/customers/7";
        perform(mvc, request(HttpMethod.valueOf(method), path).contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":" ","lastName":"","email":"not-an-email"}
                        """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    private static Customer customer(Long id) {
        Customer customer = new Customer("Ada", "Lovelace", "ada@example.com");
        customer.setId(id);
        customer.setAge(42);
        return customer;
    }

    private static String expectedCustomer(long id) {
        return """
                {"id":%d,"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","age":42}
                """.formatted(id);
    }
}
