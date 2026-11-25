package com.demo.customer;

import com.demo.testsupport.TestResultLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ExtendWith(TestResultLogger.class)
class CustomerApiApplicationTests {

    @Test
    void contextLoads() {
        com.demo.testsupport.TestHttpLogging.operation("Spring context load", null, () -> "Application context loaded");
    }

}
