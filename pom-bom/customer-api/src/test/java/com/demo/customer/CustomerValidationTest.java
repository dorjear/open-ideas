package com.demo.customer;

import com.demo.testsupport.TestResultLogger;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.demo.testsupport.TestHttpLogging.operation;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(TestResultLogger.class)
class CustomerValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsCustomerWithValidFields() {
        CustomerRequest customer = new CustomerRequest("Ada", "Lovelace", "ada@example.com");

        assertTrue(operation("validate customer", customer, () -> validator.validate(customer)).isEmpty());
    }

    @Test
    void rejectsBlankNamesAndInvalidEmail() {
        CustomerRequest customer = new CustomerRequest(" ", "", "not-an-email");

        var violations = operation("validate customer", customer, () -> validator.validate(customer));

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("firstName")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("lastName")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }
}
