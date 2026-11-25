package com.demo.testsupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Requires a buffering request factory so reading the response does not consume it. */
public class HttpExchangeLogger implements ClientHttpRequestInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(HttpExchangeLogger.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        LOG.info("REQUEST [{}] transport=HTTP method={} URL={} headers={} body={}",
                TestResultLogger.currentTest(), request.getMethod(), request.getURI(),
                headers(request.getHeaders()), new String(body, charset(request.getHeaders())));
        long started = System.nanoTime();
        try {
            ClientHttpResponse response = execution.execute(request, body);
            try {
                LOG.info("RESPONSE [{}] transport=HTTP status={} body={} durationMs={}",
                        TestResultLogger.currentTest(), response.getStatusCode().value(),
                        new String(response.getBody().readAllBytes(), charset(response.getHeaders())),
                        (System.nanoTime() - started) / 1_000_000);
                return response;
            } catch (IOException | RuntimeException | Error failure) {
                response.close();
                throw failure;
            }
        } catch (IOException | RuntimeException | Error failure) {
            LOG.error("RESPONSE [{}] transport=HTTP status=unavailable (exchange failed) URL={} durationMs={}",
                    TestResultLogger.currentTest(), request.getURI(), (System.nanoTime() - started) / 1_000_000, failure);
            throw failure;
        }
    }

    private static Charset charset(HttpHeaders headers) {
        var contentType = headers.getContentType();
        return contentType != null && contentType.getCharset() != null
                ? contentType.getCharset() : StandardCharsets.UTF_8;
    }

    private static Map<String, Object> headers(HttpHeaders headers) {
        Map<String, Object> logged = new LinkedHashMap<>();
        headers.forEach((name, values) -> logged.put(name,
                name.equalsIgnoreCase("Authorization") || name.equalsIgnoreCase("Proxy-Authorization")
                        || name.equalsIgnoreCase("Cookie") ? "[REDACTED]" : values));
        return logged;
    }
}
