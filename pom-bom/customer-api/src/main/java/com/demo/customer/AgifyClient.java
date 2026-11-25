package com.demo.customer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class AgifyClient {

    private static final Logger log = LoggerFactory.getLogger(AgifyClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public AgifyClient(RestTemplateBuilder builder,
                       @Value("${agify.base-url:https://api.agify.io}") String baseUrl,
                       @Value("${agify.api-key:}") String apiKey) {
        this.restTemplate = builder.connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10)).build();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public Integer getAge(String firstName) {
        var uri = UriComponentsBuilder.fromUriString(baseUrl).queryParam("name", "{name}");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", firstName);
        if (apiKey != null && !apiKey.isBlank()) {
            uri.queryParam("apikey", "{apiKey}");
            parameters.put("apiKey", apiKey);
        }
        URI requestUri = uri.encode().buildAndExpand(parameters).toUri();

        AgifyResponse response;
        try {
            response = restTemplate.getForObject(requestUri, AgifyResponse.class);
        } catch (RestClientException exception) {
            String status = exception instanceof RestClientResponseException httpFailure
                    ? Integer.toString(httpFailure.getStatusCode().value()) : "N/A";
            Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(exception);
            // Exception messages and request URLs can contain the API key. Log safe diagnostics only.
            log.warn("Agify age lookup failed: upstreamStatus={} failureType={} rootCauseType={}",
                    status, exception.getClass().getSimpleName(), rootCause.getClass().getSimpleName());
            if (hasTlsFailure(exception)) {
                log.warn("Agify TLS connection failed. Check that the customer API JVM trusts the "
                        + "server certificate chain, including any corporate HTTPS inspection CA.");
            }
            throw new AgifyUnavailableException(exception);
        }
        if (response == null || (response.age() != null && response.age() < 0)) {
            log.warn("Agify age lookup failed: response body is empty or contains an invalid age");
            throw new AgifyUnavailableException();
        }
        return response.age();
    }

    private static boolean hasTlsFailure(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof javax.net.ssl.SSLException) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AgifyResponse(@JsonProperty(value = "age", required = true) Integer age) {
    }
}
