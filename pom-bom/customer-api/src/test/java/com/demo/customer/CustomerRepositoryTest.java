package com.demo.customer;

import com.demo.testsupport.TestResultLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static com.demo.testsupport.TestHttpLogging.operation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ExtendWith(TestResultLogger.class)
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository repository;

    @Test
    void savesAndFindsCustomerById() {
        Customer customer = new Customer("Ada", "Lovelace", "ada@example.com");
        customer.setAge(42);
        Customer saved = operation("repository.save", customer, () -> repository.save(customer));

        var found = operation("repository.findById", saved.getId(), () -> repository.findById(saved.getId()));

        assertTrue(found.isPresent());
        assertEquals(42, found.orElseThrow().getAge());
        assertEquals("Ada", found.orElseThrow().getFirstName());
        assertEquals("Lovelace", found.orElseThrow().getLastName());
        assertEquals("ada@example.com", found.orElseThrow().getEmail());
    }
}
