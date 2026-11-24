package com.demo.customer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerRepository repository;

    @InjectMocks
    private CustomerController controller;

    @Test
    void getAllReturnsCustomersFromRepository() {
        List<Customer> customers = List.of(
                new Customer("Ada", "Lovelace", "ada@example.com"),
                new Customer("Grace", "Hopper", "grace@example.com")
        );
        when(repository.findAll()).thenReturn(customers);

        List<Customer> result = controller.getAll();

        assertSame(customers, result);
        verify(repository).findAll();
    }

    @Test
    void getByIdReturnsCustomerWhenFound() {
        Customer customer = new Customer("Ada", "Lovelace", "ada@example.com");
        when(repository.findById(7L)).thenReturn(Optional.of(customer));

        var response = controller.getById(7L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(customer, response.getBody());
    }

    @Test
    void getByIdReturnsNotFoundWhenMissing() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        var response = controller.getById(7L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void createSavesCustomerAndReturnsCreatedResponse() {
        Customer customer = new Customer("Ada", "Lovelace", "ada@example.com");
        Customer saved = new Customer("Ada", "Lovelace", "ada@example.com");
        saved.setId(9L);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/customers");
        request.setServerName("localhost");
        request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(repository.save(customer)).thenReturn(saved);

        try {
            var response = controller.create(customer);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertSame(saved, response.getBody());
            assertTrue(response.getHeaders().getLocation().toString().endsWith("/api/customers/9"));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void updateChangesAndSavesCustomerWhenFound() {
        Customer existing = new Customer("Old", "Name", "old@example.com");
        Customer updates = new Customer("Ada", "Lovelace", "ada@example.com");
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var response = controller.update(7L, updates);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(existing, response.getBody());
        assertEquals("Ada", existing.getFirstName());
        assertEquals("Lovelace", existing.getLastName());
        assertEquals("ada@example.com", existing.getEmail());
        verify(repository).save(existing);
    }

    @Test
    void updateReturnsNotFoundWhenCustomerIsMissing() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        var response = controller.update(7L, new Customer("Ada", "Lovelace", "ada@example.com"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteRemovesCustomerAndReturnsNoContentWhenFound() {
        when(repository.existsById(7L)).thenReturn(true);

        var response = controller.delete(7L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(repository).deleteById(7L);
    }

    @Test
    void deleteReturnsNotFoundWhenCustomerIsMissing() {
        when(repository.existsById(7L)).thenReturn(false);

        var response = controller.delete(7L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
