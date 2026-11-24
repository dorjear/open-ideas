package com.demo.customer;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsCustomerWithValidFields() {
        Customer customer = new Customer("Ada", "Lovelace", "ada@example.com");

        assertTrue(validator.validate(customer).isEmpty());
    }

    @Test
    void rejectsBlankNamesAndInvalidEmail() {
        Customer customer = new Customer(" ", "", "not-an-email");

        var violations = validator.validate(customer);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("firstName")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("lastName")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }
}
