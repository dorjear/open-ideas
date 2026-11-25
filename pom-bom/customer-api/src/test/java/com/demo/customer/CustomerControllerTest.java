package com.demo.customer;

import com.demo.testsupport.TestResultLogger;
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

import static com.demo.testsupport.TestHttpLogging.controllerCall;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({TestResultLogger.class, MockitoExtension.class})
class CustomerControllerTest {

    @Mock
    private CustomerService service;

    @InjectMocks
    private CustomerController controller;

    @Test
    void getAllReturnsCustomersFromService() {
        List<Customer> customers = List.of(new Customer("Ada", "Lovelace", "ada@example.com"));
        when(service.getAll()).thenReturn(customers);
        assertSame(customers, controllerCall("GET", "/api/customers", null, () -> controller.getAll()));
        verify(service).getAll();
    }

    @Test
    void getByIdReturnsCustomerWhenFound() {
        Customer customer = new Customer("Ada", "Lovelace", "ada@example.com");
        when(service.getById(7L)).thenReturn(Optional.of(customer));
        var response = controllerCall("GET", "/api/customers/7", null, () -> controller.getById(7L));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(customer, response.getBody());
    }

    @Test
    void getByIdReturnsNotFoundWhenMissing() {
        when(service.getById(7L)).thenReturn(Optional.empty());
        var response = controllerCall("GET", "/api/customers/7", null, () -> controller.getById(7L));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void createDelegatesToServiceAndReturnsLocation() {
        CustomerRequest request = new CustomerRequest("Ada", "Lovelace", "ada@example.com");
        Customer saved = new Customer("Ada", "Lovelace", "ada@example.com");
        saved.setId(9L);
        saved.setAge(42);
        when(service.create(request)).thenReturn(saved);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("POST", "/api/customers");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
        try {
            var response = controllerCall("POST", "/api/customers", request, () -> controller.create(request));
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertSame(saved, response.getBody());
            assertEquals("http://localhost/api/customers/9", response.getHeaders().getLocation().toString());
            verify(service).create(request);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void updateDelegatesToServiceWhenFound() {
        CustomerRequest request = new CustomerRequest("Ada", "Lovelace", "ada@example.com");
        Customer saved = new Customer("Ada", "Lovelace", "ada@example.com");
        saved.setAge(42);
        when(service.update(7L, request)).thenReturn(Optional.of(saved));
        var response = controllerCall("PUT", "/api/customers/7", request, () -> controller.update(7L, request));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(saved, response.getBody());
        verify(service).update(7L, request);
    }

    @Test
    void updateReturnsNotFoundWhenMissing() {
        CustomerRequest request = new CustomerRequest("Ada", "Lovelace", "ada@example.com");
        when(service.update(7L, request)).thenReturn(Optional.empty());
        var response = controllerCall("PUT", "/api/customers/7", request, () -> controller.update(7L, request));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteReturnsNoContentWhenFound() {
        when(service.delete(7L)).thenReturn(true);
        var response = controllerCall("DELETE", "/api/customers/7", null, () -> controller.delete(7L));
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service).delete(7L);
    }

    @Test
    void deleteReturnsNotFoundWhenMissing() {
        when(service.delete(7L)).thenReturn(false);
        var response = controllerCall("DELETE", "/api/customers/7", null, () -> controller.delete(7L));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
