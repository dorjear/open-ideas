package com.demo.customer;

import com.demo.testsupport.TestResultLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.List;
import java.util.Optional;

import static com.demo.testsupport.TestHttpLogging.operation;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({TestResultLogger.class, MockitoExtension.class})
class CustomerServiceTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CUSTOMER_JSON = """
            {"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com"}
            """;

    @Mock
    private CustomerRepository repository;

    @Mock
    private AgifyClient agifyClient;

    @InjectMocks
    private CustomerService service;

    @Test
    void createCustomerUsesAgifyAge() throws Exception {
        CustomerRequest request = request();
        when(agifyClient.getAge("Ada")).thenReturn(42);
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            assertNull(customer.getId());
            customer.setId(7L);
            return customer;
        });
        Customer saved = operation("CustomerService.create", request, () -> service.create(request));
        assertCustomer(saved, 42);
        var order = inOrder(agifyClient, repository);
        order.verify(agifyClient).getAge("Ada");
        order.verify(repository).save(any(Customer.class));
    }

    @Test
    void updateCustomerRefreshesAgeEvenForSameFirstName() throws Exception {
        Customer existing = storedCustomer();
        CustomerRequest request = request();
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(agifyClient.getAge("Ada")).thenReturn(42);
        when(repository.save(existing)).thenReturn(existing);
        Customer updated = operation("CustomerService.update", request,
                () -> service.update(7L, request)).orElseThrow();
        assertCustomer(updated, 42);
        verify(agifyClient).getAge("Ada");
    }

    @Test
    void createCustomerWithUnknownAge() throws Exception {
        CustomerRequest request = request();
        when(agifyClient.getAge("Ada")).thenReturn(null);
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId(7L);
            return customer;
        });
        assertCustomer(operation("CustomerService.create", request, () -> service.create(request)), null);
    }

    @Test
    void updateCustomerReplacesOldAgeWithUnknown() throws Exception {
        Customer existing = storedCustomer();
        CustomerRequest request = request();
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(agifyClient.getAge("Ada")).thenReturn(null);
        when(repository.save(existing)).thenReturn(existing);
        assertCustomer(operation("CustomerService.update", request,
                () -> service.update(7L, request)).orElseThrow(), null);
    }

    @Test
    void enquiryCustomerDoesNotCallAgify() throws Exception {
        Customer stored = storedCustomer();
        when(repository.findById(7L)).thenReturn(Optional.of(stored));
        assertCustomer(operation("CustomerService.getById", 7L,
                () -> service.getById(7L)).orElseThrow(), 17);
        verifyNoInteractions(agifyClient);
    }

    @Test
    void listCustomerDoesNotCallAgify() throws Exception {
        when(repository.findAll()).thenReturn(List.of(storedCustomer()));
        var customers = operation("CustomerService.getAll", null, () -> service.getAll());
        assertEquals(1, customers.size());
        assertCustomer(customers.getFirst(), 17);
        verifyNoInteractions(agifyClient);
    }

    @Test
    void updateMissingCustomerDoesNotCallAgify() throws Exception {
        CustomerRequest request = request();
        when(repository.findById(7L)).thenReturn(Optional.empty());
        assertTrue(operation("CustomerService.update", request, () -> service.update(7L, request)).isEmpty());
        verifyNoInteractions(agifyClient);
        verify(repository, never()).save(any(Customer.class));
    }

    @Test
    void deleteCustomerDoesNotCallAgify() {
        when(repository.existsById(7L)).thenReturn(true);
        assertTrue(operation("CustomerService.delete", 7L, () -> service.delete(7L)));
        verify(repository).deleteById(7L);
        verifyNoInteractions(agifyClient);
    }

    @Test
    void deleteMissingCustomer() {
        when(repository.existsById(7L)).thenReturn(false);
        assertFalse(operation("CustomerService.delete", 7L, () -> service.delete(7L)));
        verify(repository, never()).deleteById(7L);
        verifyNoInteractions(agifyClient);
    }

    @Test
    void agifyFailurePreventsCreate() throws Exception {
        CustomerRequest request = request();
        when(agifyClient.getAge("Ada")).thenThrow(new AgifyUnavailableException());
        assertThrows(AgifyUnavailableException.class,
                () -> operation("CustomerService.create", request, () -> service.create(request)));
        verifyNoInteractions(repository);
    }

    @Test
    void agifyFailureLeavesExistingCustomerUnchanged() throws Exception {
        Customer existing = storedCustomer();
        String originalJson = JSON.writeValueAsString(existing);
        CustomerRequest request = JSON.readValue("""
                {"firstName":"Grace","lastName":"Hopper","email":"grace@example.com"}
                """, CustomerRequest.class);
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(agifyClient.getAge("Grace")).thenThrow(new AgifyUnavailableException());
        assertThrows(AgifyUnavailableException.class,
                () -> operation("CustomerService.update", request, () -> service.update(7L, request)));
        JSONAssert.assertEquals(originalJson, JSON.writeValueAsString(existing), JSONCompareMode.STRICT);
        verify(repository, never()).save(any(Customer.class));
    }

    private static CustomerRequest request() throws Exception {
        return JSON.readValue(CUSTOMER_JSON, CustomerRequest.class);
    }

    private static Customer storedCustomer() throws Exception {
        CustomerRequest request = request();
        Customer customer = new Customer(request.firstName(), request.lastName(), request.email());
        customer.setId(7L);
        customer.setAge(17);
        return customer;
    }

    private static void assertCustomer(Customer actual, Integer age) throws Exception {
        String expected = """
                {"id":7,"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","age":%s}
                """.formatted(age);
        JSONAssert.assertEquals(expected, JSON.writeValueAsString(actual), JSONCompareMode.STRICT);
    }
}
