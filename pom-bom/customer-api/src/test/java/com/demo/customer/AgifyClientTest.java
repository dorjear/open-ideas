package com.demo.customer;

import com.demo.testsupport.HttpExchangeLogger;
import com.demo.testsupport.TestResultLogger;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static com.demo.testsupport.TestHttpLogging.operation;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith({TestResultLogger.class, OutputCaptureExtension.class})
class AgifyClientTest {

    @RegisterExtension
    static final WireMockExtension AGIFY = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().bindAddress("127.0.0.1"))
            .build();

    private AgifyClient client;

    @BeforeEach
    void setUp() {
        configureClient("");
    }

    private void configureClient(String apiKey) {
        var builder = new RestTemplateBuilder(new HttpExchangeLogger());
        client = new AgifyClient(builder, AGIFY.baseUrl(), apiKey);
    }

    @AfterEach
    void verifyRequests() {
        AGIFY.verify(1, anyRequestedFor(anyUrl()));
    }

    @Test
    void getsAgeFromFirstName() {
        AGIFY.stubFor(ageRequest("Ada").willReturn(okJson("""
                {"name":"Ada","age":42,"count":1234}
                """)));
        assertThat(operation("AgifyClient.getAge", "Ada", () -> client.getAge("Ada"))).isEqualTo(42);
    }

    @Test
    void preservesUnknownAgeAsNull() {
        AGIFY.stubFor(ageRequest("Unknown").willReturn(okJson("""
                {"name":"Unknown","age":null,"count":0}
                """)));
        assertThat(operation("AgifyClient.getAge", "Unknown", () -> client.getAge("Unknown"))).isNull();
    }

    @Test
    void encodesNameAndOptionalApiKey() {
        configureClient("test-key+");
        AGIFY.stubFor(get(urlEqualTo("/?name=Jean%20%2B%20%C3%89mile&apikey=test-key%2B"))
                .willReturn(okJson("""
                        {"name":"Jean + Émile","age":31,"count":100}
                        """)));
        assertThat(operation("AgifyClient.getAge", "Jean + Émile", () -> client.getAge("Jean + Émile")))
                .isEqualTo(31);
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 429, 500})
    void upstreamErrorsFailTheLookup(int status, CapturedOutput output) {
        AGIFY.stubFor(ageRequest("Ada").willReturn(aResponse().withStatus(status)
                .withHeader("Content-Type", "application/json").withBody("""
                        {"error":"Agify unavailable"}
                        """)));
        assertThrows(AgifyUnavailableException.class,
                () -> operation("AgifyClient.getAge", "Ada", () -> client.getAge("Ada")));
        assertThat(output).contains("Agify age lookup failed: upstreamStatus=" + status);
    }

    @Test
    @Timeout(20)
    void timeoutFailsTheLookup() {
        // Exceeds the real client's ten-second read timeout.
        AGIFY.stubFor(ageRequest("Ada").willReturn(okJson("""
                {"name":"Ada","age":42,"count":1234}
                """).withFixedDelay(10_500)));
        assertThrows(AgifyUnavailableException.class,
                () -> operation("AgifyClient.getAge", "Ada", () -> client.getAge("Ada")));
    }

    @Test
    void emptyResponseFailsTheLookup() {
        AGIFY.stubFor(ageRequest("Ada").willReturn(okJson("")));
        assertThrows(AgifyUnavailableException.class,
                () -> operation("AgifyClient.getAge", "Ada", () -> client.getAge("Ada")));
    }

    @Test
    void negativeAgeFailsTheLookup() {
        AGIFY.stubFor(ageRequest("Ada").willReturn(okJson("""
                {"name":"Ada","age":-1,"count":10}
                """)));
        assertThrows(AgifyUnavailableException.class,
                () -> operation("AgifyClient.getAge", "Ada", () -> client.getAge("Ada")));
    }

    @Test
    void missingAgeFieldFailsTheLookup() {
        AGIFY.stubFor(ageRequest("Ada").willReturn(okJson("""
                {"name":"Ada","count":100}
                """)));
        assertThrows(AgifyUnavailableException.class,
                () -> operation("AgifyClient.getAge", "Ada", () -> client.getAge("Ada")));
    }

    @Test
    void malformedResponseFailsTheLookup() {
        AGIFY.stubFor(ageRequest("Ada").willReturn(okJson("not-json")));
        assertThrows(AgifyUnavailableException.class,
                () -> operation("AgifyClient.getAge", "Ada", () -> client.getAge("Ada")));
    }

    private static MappingBuilder ageRequest(String name) {
        return get(urlPathEqualTo("/"))
                .withQueryParam("name", equalTo(name))
                .withQueryParam("apikey", absent());
    }
}
