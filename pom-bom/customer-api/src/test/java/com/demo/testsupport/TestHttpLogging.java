package com.demo.testsupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Test-only diagnostics; it does not replace requests or assertions. */
public final class TestHttpLogging {

    private static final Logger LOG = LoggerFactory.getLogger(TestHttpLogging.class);
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    private TestHttpLogging() {
    }

    public static ResultActions perform(MockMvc mvc, RequestBuilder request) throws Exception {
        long started = System.nanoTime();
        try {
            return mvc.perform(request).andDo(result -> {
                var incoming = result.getRequest();
                String url = incoming.getRequestURL().toString();
                if (incoming.getQueryString() != null) {
                    url += "?" + incoming.getQueryString();
                }
                Map<String, Object> headers = new LinkedHashMap<>();
                for (String name : Collections.list(incoming.getHeaderNames())) {
                    headers.put(name, sensitive(name) ? "[REDACTED]" : Collections.list(incoming.getHeaders(name)));
                }
                LOG.info("REQUEST [{}] transport=MockMvc method={} URL={} headers={} body={}",
                        TestResultLogger.currentTest(), incoming.getMethod(), url, headers,
                        requestBody(incoming.getContentAsByteArray(), incoming.getCharacterEncoding()));
                var outgoing = result.getResponse();
                LOG.info("RESPONSE [{}] transport=MockMvc status={} body={} durationMs={}",
                        TestResultLogger.currentTest(), outgoing.getStatus(), outgoing.getContentAsString(StandardCharsets.UTF_8),
                        (System.nanoTime() - started) / 1_000_000);
            });
        } catch (Exception | AssertionError failure) {
            LOG.error("MockMvc exchange failed [{}] before response logging", TestResultLogger.currentTest(), failure);
            throw failure;
        }
    }

    public static <T> T controllerCall(String method, String path, Object body, Supplier<T> action) {
        LOG.info("REQUEST [{}] transport=direct-controller method={} URL={} (logical endpoint) "
                        + "headers=N/A (no HTTP request) body={}",
                TestResultLogger.currentTest(), method, path, json(body));
        return invoke("direct-controller", action);
    }

    public static <T> T operation(String name, Object input, Supplier<T> action) {
        LOG.info("REQUEST [{}] operation={} URL=N/A headers=N/A (no HTTP request) body={}",
                TestResultLogger.currentTest(), name, json(input));
        return invoke(name, action);
    }

    private static <T> T invoke(String operation, Supplier<T> action) {
        long started = System.nanoTime();
        try {
            T result = action.get();
            Object status = "N/A (no HTTP response)";
            Object body = result;
            if (result instanceof ResponseEntity<?> response) {
                status = response.getStatusCode().value();
                body = response.getBody();
            }
            LOG.info("RESPONSE [{}] operation={} status={} body={} durationMs={}",
                    TestResultLogger.currentTest(), operation, status, json(body),
                    (System.nanoTime() - started) / 1_000_000);
            return result;
        } catch (RuntimeException | Error failure) {
            LOG.error("RESPONSE [{}] operation={} status=unavailable (exception) durationMs={}",
                    TestResultLogger.currentTest(), operation, (System.nanoTime() - started) / 1_000_000, failure);
            throw failure;
        }
    }

    private static String requestBody(byte[] body, String encoding) {
        if (body == null || body.length == 0) {
            return "<empty>";
        }
        return new String(body, encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding));
    }

    private static String json(Object value) {
        if (value instanceof java.util.Set<?> values
                && values.stream().allMatch(jakarta.validation.ConstraintViolation.class::isInstance)) {
            value = values.stream().map(item -> {
                var violation = (jakarta.validation.ConstraintViolation<?>) item;
                return violation.getPropertyPath() + ": " + violation.getMessage();
            }).toList();
        }
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            return "[not JSON serializable: " + value.getClass().getSimpleName() + "]";
        }
    }

    private static boolean sensitive(String name) {
        return name.equalsIgnoreCase("Authorization") || name.equalsIgnoreCase("Proxy-Authorization")
                || name.equalsIgnoreCase("Cookie") || name.equalsIgnoreCase("Set-Cookie");
    }
}
