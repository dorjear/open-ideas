package com.demo.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository repository;

    @Test
    void savesAndFindsCustomerById() {
        Customer saved = repository.save(new Customer("Ada", "Lovelace", "ada@example.com"));

        var found = repository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Ada", found.orElseThrow().getFirstName());
        assertEquals("Lovelace", found.orElseThrow().getLastName());
        assertEquals("ada@example.com", found.orElseThrow().getEmail());
    }
}
